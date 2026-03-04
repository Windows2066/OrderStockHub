package com.example.project1.persistence.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单中单个产品行的实体类。
 * 此实体对应数据库中的{@code t_order_item}表。每个记录代表订单中的一个特定SKU（产品变体），包括数量、
 * 价格和产品细节。这允许复杂订单的组成和准确的库存扣减。
 * 关键特性：
 * - 通过orderId链接到父订单。
 * - 在订单时间存储产品信息的快照以确保历史准确性。
 * - 支持每个订单的多个项。
 */
public class OrderItemEntity {

    /** 数据库主键。自动生成的订单项记录唯一标识符。 */
    private Long id;

    /** 外键，引用父订单的ID（{@code t_order.id}）。 */
    private Long orderId;

    /** 此订单项中产品变体的SKU代码。 */
    private String skuCode;

    /** 在下单时获取的产品名称快照。 */
    private String skuName;

    /** 在下单时的产品单价。 */
    private BigDecimal price;

    /** 此订单项中购买的产品数量。 */
    private Integer quantity;

    /** 订单项创建的时间戳。 */
    private LocalDateTime createdAt;

    /** 订单项最后更新的时间戳。 */
    private LocalDateTime updatedAt;

    /**
     * 获取主键ID。
     *
     * @return 订单项的ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键ID。
     *
     * @param id 要设置的ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取关联的订单ID。
     *
     * @return 父订单的ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * 设置关联的订单ID。
     *
     * @param orderId 要设置的订单ID
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * 获取SKU代码。
     *
     * @return 产品的SKU代码
     */
    public String getSkuCode() {
        return skuCode;
    }

    /**
     * 设置SKU代码。
     *
     * @param skuCode 要设置的SKU代码
     */
    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    /**
     * 获取SKU名称快照。
     *
     * @return 订单时间的商品名称
     */
    public String getSkuName() {
        return skuName;
    }

    /**
     * 设置SKU名称快照。
     *
     * @param skuName 要设置的SKU名称
     */
    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    /**
     * 获取单价。
     *
     * @return 订单时间的每单位价格
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * 设置单价。
     *
     * @param price 要设置的单价
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * 获取数量。
     *
     * @return 购买的数量
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 设置数量。
     *
     * @param quantity 要设置的数量
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 获取创建时间戳。
     *
     * @return 订单项的创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间戳。
     *
     * @param createdAt 要设置的创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取最后更新时间戳。
     *
     * @return 订单项的最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置最后更新时间戳。
     *
     * @param updatedAt 要设置的最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
