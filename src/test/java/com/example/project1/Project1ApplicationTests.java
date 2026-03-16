package com.example.project1;

import com.example.project1.dto.CreateOrderRequest;
import com.example.project1.exception.BusinessException;
import com.example.project1.persistence.mapper.InventoryMapper;
import com.example.project1.persistence.mapper.OrderMapper;
import com.example.project1.persistence.mapper.OutboxEventMapper;
import com.example.project1.persistence.model.InventoryEntity;
import com.example.project1.persistence.model.OrderEntity;
import com.example.project1.persistence.model.OutboxEventEntity;
import com.example.project1.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
class Project1ApplicationTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Test
    void contextLoads() {
        Assertions.assertNotNull(orderService);
    }

    @Test
    void createOrderShouldWriteOutboxEvent() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRequestId("TEST-REQ-" + System.currentTimeMillis());
        request.setUserId(1L);

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1001L);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("9.90"));
        request.setItems(List.of(item));

        OrderEntity order = orderService.createOrder(request);
        Assertions.assertNotNull(order);
        Assertions.assertNotNull(order.getOrderNo());

        OutboxEventEntity outboxEvent = outboxEventMapper.selectByEventTypeAndBizKey("ORDER_CREATED", order.getOrderNo());
        Assertions.assertNotNull(outboxEvent);
        Assertions.assertEquals(0, outboxEvent.getStatus());
    }

    @Test
    void markPublishedShouldResetRetryFields() {
        String bizKey = "TEST-OUTBOX-" + System.currentTimeMillis();

        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setEventType("ORDER_CREATED");
        entity.setBizKey(bizKey);
        entity.setTopic("order-created-topic");
        entity.setTags("created");
        entity.setPayload("{\"orderNo\":\"" + bizKey + "\"}");
        entity.setStatus(0);
        entity.setRetryCount(0);
        outboxEventMapper.insert(entity);

        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(5);
        outboxEventMapper.markFailed(entity.getId(), nextRetryAt, "模拟一次发送失败");
        outboxEventMapper.markPublished(entity.getId());

        OutboxEventEntity updated = outboxEventMapper.selectByEventTypeAndBizKey("ORDER_CREATED", bizKey);
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(1, updated.getStatus());
        Assertions.assertEquals(0, updated.getRetryCount());
        Assertions.assertNull(updated.getNextRetryAt());
        Assertions.assertNull(updated.getLastError());
    }

    @Test
    void createOrderShouldBeIdempotentByRequestId() {
        ensureInventoryAtLeast("SKU-1001", 2);
        int beforeQty = inventoryMapper.selectBySkuCode("SKU-1001").getAvailableQty();

        String requestId = "TEST-IDEMP-" + System.currentTimeMillis();
        CreateOrderRequest request = buildSingleItemRequest(requestId, 1);

        OrderEntity firstOrder = orderService.createOrder(request);
        OrderEntity secondOrder = orderService.createOrder(request);

        Assertions.assertNotNull(firstOrder);
        Assertions.assertNotNull(secondOrder);
        Assertions.assertEquals(firstOrder.getOrderNo(), secondOrder.getOrderNo());

        int afterQty = inventoryMapper.selectBySkuCode("SKU-1001").getAvailableQty();
        Assertions.assertEquals(beforeQty - 1, afterQty);
    }

    @Test
    void createOrderShouldRollbackWhenInventoryInsufficient() {
        ensureInventoryAtLeast("SKU-1001", 1);
        int beforeQty = inventoryMapper.selectBySkuCode("SKU-1001").getAvailableQty();

        String requestId = "TEST-NOSTOCK-" + System.currentTimeMillis();
        CreateOrderRequest request = buildSingleItemRequest(requestId, beforeQty + 1);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> orderService.createOrder(request));
        Assertions.assertEquals(4102, ex.getCode());

        OrderEntity created = orderMapper.selectByRequestId(requestId);
        Assertions.assertNull(created);

        int afterQty = inventoryMapper.selectBySkuCode("SKU-1001").getAvailableQty();
        Assertions.assertEquals(beforeQty, afterQty);
    }

    private CreateOrderRequest buildSingleItemRequest(String requestId, int quantity) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRequestId(requestId);
        request.setUserId(1L);

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1001L);
        item.setQuantity(quantity);
        item.setPrice(new BigDecimal("9.90"));
        request.setItems(List.of(item));
        return request;
    }

    private void ensureInventoryAtLeast(String skuCode, int minQty) {
        InventoryEntity inventory = inventoryMapper.selectBySkuCode(skuCode);
        Assertions.assertNotNull(inventory, "测试库存SKU不存在: " + skuCode);
        if (inventory.getAvailableQty() < minQty) {
            inventoryMapper.rollbackAvailable(skuCode, minQty - inventory.getAvailableQty());
        }
    }
}
