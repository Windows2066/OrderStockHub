package com.example.project1.service;

import com.example.project1.dto.CreateOrderRequest;
import com.example.project1.dto.OrderDetailResponse;
import com.example.project1.exception.BusinessException;
import com.example.project1.persistence.mapper.InventoryLogMapper;
import com.example.project1.persistence.mapper.InventoryMapper;
import com.example.project1.persistence.mapper.OrderItemMapper;
import com.example.project1.persistence.mapper.OrderMapper;
import com.example.project1.persistence.model.InventoryEntity;
import com.example.project1.persistence.model.InventoryLogEntity;
import com.example.project1.persistence.model.OrderEntity;
import com.example.project1.persistence.model.OrderItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 订单业务服务。
 *
 * 该服务负责实现完整下单链路：
 * 1. 请求幂等校验，避免重复下单
 * 2. 创建订单主记录
 * 3. 创建订单明细记录
 * 4. 扣减库存（防超卖）
 * 5. 写入库存流水
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /**
     * 订单主表Mapper。
     */
    private final OrderMapper orderMapper;

    /**
     * 订单明细表Mapper。
     */
    private final OrderItemMapper orderItemMapper;

    /**
     * 库存表Mapper。
     */
    private final InventoryMapper inventoryMapper;

    /**
     * 库存流水表Mapper。
     */
    private final InventoryLogMapper inventoryLogMapper;

    public OrderService(OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper,
                        InventoryMapper inventoryMapper,
                        InventoryLogMapper inventoryLogMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryLogMapper = inventoryLogMapper;
    }

    /**
     * 创建订单（下单+扣库存+库存流水）。
     *
     * 该方法使用事务保证原子性：任意一步失败都会回滚，避免出现
     * "订单已创建但库存未扣减" 或 "库存扣减了但订单未落库" 的不一致状态。
     *
     * @param request 下单请求参数
     * @return 创建后的订单主记录
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderEntity createOrder(CreateOrderRequest request) {
        String requestId = request.getRequestId();

        // 先做幂等查询：若同一requestId已成功下单，直接返回历史结果。
        OrderEntity existed = orderMapper.selectByRequestId(requestId);
        if (existed != null) {
            log.info("幂等命中，直接返回已有订单，requestId={}, orderNo={}", requestId, existed.getOrderNo());
            return existed;
        }

        // 业务订单号采用UUID，避免高并发下毫秒时间戳重复。
        String orderNo = "ORD" + UUID.randomUUID().toString().replace("-", "");
        try {
            // 汇总订单总金额 = sum(单价 * 数量)。
            BigDecimal totalAmount = request.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 先写入订单主表，依赖request_id唯一约束兜底并发幂等。
            OrderEntity order = new OrderEntity();
            order.setOrderNo(orderNo);
            order.setRequestId(requestId);
            order.setUserId(request.getUserId());
            order.setStatus(0);
            order.setTotalAmount(totalAmount);
            try {
                orderMapper.insert(order);
            } catch (DuplicateKeyException duplicateKeyException) {
                // 并发重复请求时，唯一索引会拦截；此时返回已存在订单。
                OrderEntity duplicatedOrder = orderMapper.selectByRequestId(requestId);
                if (duplicatedOrder != null) {
                    log.info("并发幂等命中，返回已存在订单，requestId={}, orderNo={}", requestId, duplicatedOrder.getOrderNo());
                    return duplicatedOrder;
                }
                log.error("订单插入触发唯一约束但未查到历史订单，requestId={}, 异常={}", requestId, duplicateKeyException.getMessage(), duplicateKeyException);
                throw duplicateKeyException;
            }

            // 组装并批量写入订单明细。
            List<OrderItemEntity> orderItems = new ArrayList<>();
            for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
                OrderItemEntity orderItem = new OrderItemEntity();
                orderItem.setOrderId(order.getId());
                orderItem.setSkuCode(buildSkuCode(item.getProductId()));
                // 当前阶段没有独立商品中心，名称先用“商品+ID”做快照占位。
                orderItem.setSkuName("商品" + item.getProductId());
                orderItem.setPrice(item.getPrice());
                orderItem.setQuantity(item.getQuantity());
                orderItems.add(orderItem);
            }
            orderItemMapper.batchInsert(orderItems);

            // 为降低多SKU并发死锁概率，按productId升序处理库存扣减。
            List<CreateOrderRequest.OrderItemRequest> sortedItems = new ArrayList<>(request.getItems());
            sortedItems.sort(Comparator.comparing(CreateOrderRequest.OrderItemRequest::getProductId));

            // 逐项扣库存并写库存流水，确保每个SKU都有可追溯记录。
            for (CreateOrderRequest.OrderItemRequest item : sortedItems) {
                String skuCode = buildSkuCode(item.getProductId());

                // 在事务中对库存行加锁，保证读取快照与后续扣减一致。
                InventoryEntity inventory = inventoryMapper.selectBySkuCodeForUpdate(skuCode);
                if (inventory == null) {
                    log.warn("下单失败，库存不存在，orderNo={}, skuCode={}", orderNo, skuCode);
                    throw new BusinessException(4101, "商品" + skuCode + "库存不存在");
                }
                if (inventory.getAvailableQty() < item.getQuantity()) {
                    log.warn("下单失败，库存不足，orderNo={}, skuCode={}, beforeQty={}, needQty={}",
                            orderNo, skuCode, inventory.getAvailableQty(), item.getQuantity());
                    throw new BusinessException(4102, "商品" + skuCode + "库存不足");
                }

                int beforeQty = inventory.getAvailableQty();

                // 防超卖SQL：仅在available_qty >= 扣减数量时才更新成功。
                int updated = inventoryMapper.deductAvailable(skuCode, item.getQuantity());
                if (updated == 0) {
                    log.warn("下单失败，条件扣减未命中，判定库存不足，orderNo={}, skuCode={}, beforeQty={}, needQty={}",
                            orderNo, skuCode, beforeQty, item.getQuantity());
                    throw new BusinessException(4102, "商品" + skuCode + "库存不足");
                }

                int afterQty = beforeQty - item.getQuantity();
                log.info("库存扣减成功，orderNo={}, skuCode={}, beforeQty={}, deductQty={}, afterQty={}",
                        orderNo, skuCode, beforeQty, item.getQuantity(), afterQty);

                // 写库存流水。change_qty按表设计使用正数，变更方向由change_type表达。
                InventoryLogEntity inventoryLog = new InventoryLogEntity();
                inventoryLog.setBizNo(orderNo);
                inventoryLog.setSkuCode(skuCode);
                inventoryLog.setChangeType("DEDUCT");
                inventoryLog.setChangeQty(item.getQuantity());
                inventoryLog.setBeforeQty(beforeQty);
                inventoryLog.setAfterQty(afterQty);
                inventoryLog.setRemark("下单扣减库存");
                inventoryLogMapper.insert(inventoryLog);
            }

            log.info("下单成功，requestId={}, orderNo={}, userId={}, itemCount={}",
                    requestId, orderNo, request.getUserId(), request.getItems().size());
            return order;
        } catch (BusinessException businessException) {
            log.warn("下单业务异常，requestId={}, orderNo={}, 原因={}",
                    requestId, orderNo, businessException.getMessage());
            throw businessException;
        } catch (Exception exception) {
            log.error("下单系统异常，requestId={}, orderNo={}, 原因={}",
                    requestId, orderNo, exception.getMessage(), exception);
            throw exception;
        }
    }

    /**
     * 查询订单详情（订单主信息 + 订单项）。
     *
     * @param orderNo 业务订单号
     * @return 聚合后的订单详情
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(String orderNo) {
        OrderEntity order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(4204, "订单不存在");
        }

        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrder(order);
        response.setItems(orderItemMapper.selectByOrderId(order.getId()));
        return response;
    }

    /**
     * 按商品ID查询库存。
     *
     * @param productId 商品ID
     * @return 库存信息
     */
    @Transactional(readOnly = true)
    public InventoryEntity getInventoryByProductId(Long productId) {
        String skuCode = buildSkuCode(productId);
        InventoryEntity inventory = inventoryMapper.selectBySkuCode(skuCode);
        if (inventory == null) {
            throw new BusinessException(4101, "商品" + skuCode + "库存不存在");
        }
        return inventory;
    }

    /**
     * 统一构造库存SKU编码。
     *
     * 数据库初始化脚本使用格式：SKU-1001，
     * 因此这里也统一按SKU-商品ID拼接，避免查询不到库存。
     */
    private String buildSkuCode(Long productId) {
        return "SKU-" + productId;
    }
}
