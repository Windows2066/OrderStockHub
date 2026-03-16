package com.example.project1.service;

import com.example.project1.dto.CreateOrderRequest;
import com.example.project1.dto.OrderDetailResponse;
import com.example.project1.exception.BusinessException;
import com.example.project1.persistence.mapper.InventoryLogMapper;
import com.example.project1.persistence.mapper.InventoryMapper;
import com.example.project1.persistence.mapper.OrderItemMapper;
import com.example.project1.persistence.mapper.OrderMapper;
import com.example.project1.persistence.mapper.OutboxEventMapper;
import com.example.project1.persistence.model.InventoryEntity;
import com.example.project1.persistence.model.InventoryLogEntity;
import com.example.project1.persistence.model.OrderEntity;
import com.example.project1.persistence.model.OrderItemEntity;
import com.example.project1.persistence.model.OutboxEventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 订单业务服务。
 *
 * 该服务负责实现完整下单链路：
 * 1. 幂等与并发控制（Redisson + 唯一索引）
 * 2. 创建订单主记录与明细
 * 3. 扣减库存（防超卖）
 * 4. 写入库存流水
 * 5. 同事务写入 Outbox 事件
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Value("${app.rocketmq.order-topic:order-created-topic}")
    private String orderCreatedTopic;

    @Value("${app.rocketmq.order-tags:created}")
    private String orderCreatedTags;

    @Value("${app.redis.lock.order-create.prefix:lock:order:create:}")
    private String orderCreateLockPrefix;

    @Value("${app.redis.lock.order-create.wait-seconds:2}")
    private long orderCreateLockWaitSeconds;

    @Value("${app.redis.lock.order-create.lease-seconds:10}")
    private long orderCreateLockLeaseSeconds;

    public OrderService(OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper,
                        InventoryMapper inventoryMapper,
                        InventoryLogMapper inventoryLogMapper,
                        OutboxEventMapper outboxEventMapper,
                        RedissonClient redissonClient,
                        ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.outboxEventMapper = outboxEventMapper;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建订单（下单+扣库存+库存流水+Outbox）。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderEntity createOrder(CreateOrderRequest request) {
        String requestId = request.getRequestId();
        String lockKey = orderCreateLockPrefix + requestId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked;
        try {
            // 先尝试获取 requestId 级分布式锁，避免并发重复下单穿透到数据库层。
            locked = lock.tryLock(orderCreateLockWaitSeconds, orderCreateLockLeaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.warn("下单获取分布式锁被中断，requestId={}, lockKey={}", requestId, lockKey);
            throw new BusinessException(4301, "系统繁忙，请稍后重试");
        }

        if (!locked) {
            log.warn("下单获取分布式锁失败，requestId={}, lockKey={}, waitSeconds={}, leaseSeconds={}",
                    requestId, lockKey, orderCreateLockWaitSeconds, orderCreateLockLeaseSeconds);
            throw new BusinessException(4302, "请求处理中，请勿重复提交");
        }

        String orderNo = "ORD" + UUID.randomUUID().toString().replace("-", "");
        try {
            OrderEntity existed = orderMapper.selectByRequestId(requestId);
            if (existed != null) {
                log.info("幂等命中，直接返回已有订单，requestId={}, orderNo={}", requestId, existed.getOrderNo());
                return existed;
            }

            BigDecimal totalAmount = request.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            OrderEntity order = new OrderEntity();
            order.setOrderNo(orderNo);
            order.setRequestId(requestId);
            order.setUserId(request.getUserId());
            order.setStatus(0);
            order.setTotalAmount(totalAmount);
            try {
                orderMapper.insert(order);
            } catch (DuplicateKeyException duplicateKeyException) {
                OrderEntity duplicatedOrder = orderMapper.selectByRequestId(requestId);
                if (duplicatedOrder != null) {
                    log.info("并发幂等命中，返回已存在订单，requestId={}, orderNo={}", requestId, duplicatedOrder.getOrderNo());
                    return duplicatedOrder;
                }
                throw duplicateKeyException;
            }

            List<OrderItemEntity> orderItems = new ArrayList<>();
            for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
                OrderItemEntity orderItem = new OrderItemEntity();
                orderItem.setOrderId(order.getId());
                orderItem.setSkuCode(buildSkuCode(item.getProductId()));
                orderItem.setSkuName("商品" + item.getProductId());
                orderItem.setPrice(item.getPrice());
                orderItem.setQuantity(item.getQuantity());
                orderItems.add(orderItem);
            }
            orderItemMapper.batchInsert(orderItems);

            List<CreateOrderRequest.OrderItemRequest> sortedItems = new ArrayList<>(request.getItems());
            sortedItems.sort(Comparator.comparing(CreateOrderRequest.OrderItemRequest::getProductId));

            for (CreateOrderRequest.OrderItemRequest item : sortedItems) {
                String skuCode = buildSkuCode(item.getProductId());
                InventoryEntity inventory = inventoryMapper.selectBySkuCodeForUpdate(skuCode);
                if (inventory == null) {
                    throw new BusinessException(4101, "商品" + skuCode + "库存不存在");
                }
                if (inventory.getAvailableQty() < item.getQuantity()) {
                    throw new BusinessException(4102, "商品" + skuCode + "库存不足");
                }

                int beforeQty = inventory.getAvailableQty();
                int updated = inventoryMapper.deductAvailable(skuCode, item.getQuantity());
                if (updated == 0) {
                    throw new BusinessException(4102, "商品" + skuCode + "库存不足");
                }

                int afterQty = beforeQty - item.getQuantity();
                log.info("库存扣减成功，orderNo={}, skuCode={}, beforeQty={}, deductQty={}, afterQty={}",
                        orderNo, skuCode, beforeQty, item.getQuantity(), afterQty);

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

            // 下单事务内写 Outbox，确保业务数据与待发送事件一起提交。
            OutboxEventEntity outboxEvent = new OutboxEventEntity();
            outboxEvent.setEventType("ORDER_CREATED");
            outboxEvent.setBizKey(orderNo);
            outboxEvent.setTopic(orderCreatedTopic);
            outboxEvent.setTags(orderCreatedTags);
            outboxEvent.setPayload(buildOrderCreatedPayload(order, request));
            outboxEvent.setStatus(0);
            outboxEvent.setRetryCount(0);
            outboxEvent.setNextRetryAt(LocalDateTime.now());
            outboxEventMapper.insert(outboxEvent);

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
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

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

    @Transactional(readOnly = true)
    public InventoryEntity getInventoryByProductId(Long productId) {
        String skuCode = buildSkuCode(productId);
        InventoryEntity inventory = inventoryMapper.selectBySkuCode(skuCode);
        if (inventory == null) {
            throw new BusinessException(4101, "商品" + skuCode + "库存不存在");
        }
        return inventory;
    }

    private String buildSkuCode(Long productId) {
        return "SKU-" + productId;
    }

    private String buildOrderCreatedPayload(OrderEntity order, CreateOrderRequest request) {
        try {
            return objectMapper.writeValueAsString(new OrderCreatedPayload(order.getOrderNo(), request.getRequestId(), order.getUserId(), order.getTotalAmount()));
        } catch (JsonProcessingException jsonProcessingException) {
            throw new BusinessException(4303, "订单事件序列化失败");
        }
    }

    /**
     * 订单创建事件最小载荷。
     */
    private record OrderCreatedPayload(String orderNo, String requestId, Long userId, BigDecimal totalAmount) {
    }
}
