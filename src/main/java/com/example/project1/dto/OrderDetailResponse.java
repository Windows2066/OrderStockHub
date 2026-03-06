package com.example.project1.dto;

import com.example.project1.persistence.model.OrderEntity;
import com.example.project1.persistence.model.OrderItemEntity;

import java.util.List;

/**
 * 订单详情响应DTO。
 * <p>
 * 用于聚合订单主信息与订单明细列表，
 * 便于前端一次请求拿到完整订单数据。
 */
public class OrderDetailResponse {

    /** 订单主信息。 */
    private OrderEntity order;

    /** 订单明细列表。 */
    private List<OrderItemEntity> items;

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }

    public void setItems(List<OrderItemEntity> items) {
        this.items = items;
    }
}

