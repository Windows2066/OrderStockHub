package com.example.project1.controller;

import com.example.project1.dto.CreateOrderRequest;
import com.example.project1.persistence.model.OrderEntity;
import com.example.project1.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器。
 *
 * 对外提供订单相关HTTP接口，当前实现下单接口POST /orders。
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    /**
     * 订单业务服务。
     */
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 下单接口。
     *
     * 请求体示例：
     * {
     *   "userId": 1,
     *   "items": [
     *     {"productId": 1001, "quantity": 2, "price": 10.50}
     *   ]
     * }
     *
     * 该接口会触发：创建订单、扣减库存、记录库存流水。
     *
     * @param request 下单请求参数
     * @return 创建成功后的订单主记录
     */
    @PostMapping
    public ResponseEntity<OrderEntity> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderEntity order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }
}

