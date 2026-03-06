package com.example.project1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单请求DTO。
 * <p>
 * 该对象用于承接客户端传入的下单参数，
 * 包含请求幂等号、下单用户ID和订单项列表。
 */
public class CreateOrderRequest {

    /**
     * 请求幂等号。
     *
     * 约定：同一次业务重试必须传相同requestId，
     * 以便服务端识别重复请求并避免重复创建订单。
     */
    @NotBlank(message = "requestId不能为空")
    @Size(max = 64, message = "requestId长度不能超过64")
    private String requestId;

    /**
     * 下单用户ID。
     */
    @NotNull(message = "userId不能为空")
    @Min(value = 1, message = "userId必须大于等于1")
    private Long userId;

    /**
     * 订单项列表，至少包含一项。
     */
    @NotEmpty(message = "items不能为空")
    @Valid
    private List<OrderItemRequest> items;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    /**
     * 订单项请求对象。
     *
     * 每一项表示要购买的一个商品及其数量、下单价格。
     */
    public static class OrderItemRequest {

        /**
         * 商品ID。
         *
         * 该项目中会转换为库存SKU：SKU-商品ID。
         */
        @NotNull(message = "productId不能为空")
        @Min(value = 1, message = "productId必须大于等于1")
        private Long productId;

        /**
         * 购买数量，必须大于等于1。
         */
        @NotNull(message = "quantity不能为空")
        @Min(value = 1, message = "quantity必须大于等于1")
        private Integer quantity;

        /**
         * 下单单价，必须大于0，且最多保留两位小数。
         */
        @NotNull(message = "price不能为空")
        @DecimalMin(value = "0.01", message = "price必须大于0")
        @Digits(integer = 16, fraction = 2, message = "price最多16位整数且最多2位小数")
        private BigDecimal price;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
