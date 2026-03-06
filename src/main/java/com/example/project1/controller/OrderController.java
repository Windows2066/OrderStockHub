package com.example.project1.controller;

import com.example.project1.common.ApiResponse;
import com.example.project1.dto.CreateOrderRequest;
import com.example.project1.dto.OrderDetailResponse;
import com.example.project1.persistence.model.InventoryEntity;
import com.example.project1.persistence.model.OrderEntity;
import com.example.project1.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器。
 * <p>
 * 对外提供订单相关HTTP接口：
 * 1. POST /orders 下单
 * 2. GET /orders/{orderNo} 订单详情
 * 3. GET /orders/inventory/{productId} 库存查询
 */
@RestController
@RequestMapping("/orders")
@Validated
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
     *   "requestId": "REQ-20260305-0001",
     *   "userId": 1,
     *   "items": [
     *     {"productId": 1001, "quantity": 2, "price": 10.50}
     *   ]
     * }
     *
     * 该接口会触发：创建订单、扣减库存、记录库存流水。
     *
     * @param request 下单请求参数
     * @return 统一响应格式，data为创建成功后的订单主记录
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderEntity>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderEntity order = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 订单详情查询接口。
     *
     * @param orderNo 业务订单号
     * @return 订单主信息与订单明细
     */
    @GetMapping("/{orderNo}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(@PathVariable String orderNo) {
        OrderDetailResponse orderDetail = orderService.getOrderDetail(orderNo);
        return ResponseEntity.ok(ApiResponse.success(orderDetail));
    }

    /**
     * 库存查询接口。
     *
     * @param productId 商品ID
     * @return 对应商品库存信息
     */
    @GetMapping("/inventory/{productId}")
    public ResponseEntity<ApiResponse<InventoryEntity>> getInventory(@PathVariable @Min(value = 1, message = "productId必须大于等于1") Long productId) {
        InventoryEntity inventory = orderService.getInventoryByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }
}
