package com.example.project1.service;

import com.example.project1.dto.CreateOrderRequest;
import com.example.project1.persistence.mapper.InventoryLogMapper;
import com.example.project1.persistence.mapper.InventoryMapper;
import com.example.project1.persistence.mapper.OrderItemMapper;
import com.example.project1.persistence.mapper.OrderMapper;
import com.example.project1.persistence.model.InventoryEntity;
import com.example.project1.persistence.model.InventoryLogEntity;
import com.example.project1.persistence.model.OrderEntity;
import com.example.project1.persistence.model.OrderItemEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单业务服务。
 *
 * 该服务负责实现完整下单链路：
 * 1. 校验库存是否充足
 * 2. 创建订单主记录
 * 3. 创建订单明细记录
 * 4. 扣减库存
 * 5. 写入库存流水
 */
@Service
public class OrderService {

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
        // 业务订单号：这里采用时间戳简化实现，便于本地联调。
        String orderNo = "ORD" + System.currentTimeMillis();

        // 汇总订单总金额 = sum(单价 * 数量)。
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 先做库存预检查，尽量在写订单前提前失败，减少无效写入。
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            String skuCode = buildSkuCode(item.getProductId());
            InventoryEntity inventory = inventoryMapper.selectBySkuCode(skuCode);
            if (inventory == null) {
                throw new RuntimeException("商品" + skuCode + "库存不存在");
            }
            if (inventory.getAvailableQty() < item.getQuantity()) {
                throw new RuntimeException("商品" + skuCode + "库存不足");
            }
        }

        // 写入订单主表。
        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setStatus(0);
        order.setTotalAmount(totalAmount);
        orderMapper.insert(order);

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

        // 逐项扣库存并写库存流水，确保每个SKU都有可追溯记录。
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            String skuCode = buildSkuCode(item.getProductId());
            InventoryEntity inventory = inventoryMapper.selectBySkuCode(skuCode);
            int beforeQty = inventory.getAvailableQty();

            // 条件更新扣库存：仅在available_qty足够时扣减成功。
            int updated = inventoryMapper.deductAvailable(skuCode, item.getQuantity());
            if (updated == 0) {
                throw new RuntimeException("商品" + skuCode + "扣库存失败");
            }

            // 写库存流水。change_qty按表设计使用正数，变更方向由change_type表达。
            InventoryLogEntity log = new InventoryLogEntity();
            log.setBizNo(orderNo);
            log.setSkuCode(skuCode);
            log.setChangeType("DEDUCT");
            log.setChangeQty(item.getQuantity());
            log.setBeforeQty(beforeQty);
            log.setAfterQty(beforeQty - item.getQuantity());
            log.setRemark("下单扣减库存");
            inventoryLogMapper.insert(log);
        }

        return order;
    }

    /**
     * 统一构造库存SKU编码。
     *
     * 数据库初始化脚本使用格式：SKU-1001，
     * 因此这里也统一按 SKU-商品ID 拼接，避免查询不到库存。
     */
    private String buildSkuCode(Long productId) {
        return "SKU-" + productId;
    }
}

