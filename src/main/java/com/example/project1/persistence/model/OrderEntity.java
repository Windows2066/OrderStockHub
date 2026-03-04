package com.example.project1.persistence.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电商系统中订单主要信息的实体类。
 * 此实体对应数据库中的{@code t_order}表。它存储客户订单的核心细节，包括标识符、状态和财务信息。
 * 此实体作为订单管理的中心记录，链接到订单项和库存操作。
 * 关键职责：
 * - 跟踪从创建到履行的订单生命周期。
 * - 通过订单号确保唯一性和可追溯性。
 * - 维护财务准确性，包括总金额。
 */
public class OrderEntity {

    /** 数据库主键。自动生成的订单记录唯一标识符。 */
    private Long id;

    /** 业务订单号，用于幂等和跟踪。在整个系统中必须全局唯一。 */
    private String orderNo;

    /** 下单用户的ID。 */
    private Long userId;

    /**
     * 订单状态代码，表示订单的当前状态。
     *     0 - CREATED：订单已创建但尚未支付。
     *     1 - PAID：订单已支付并正在处理。
     *     2 - CANCELED：订单已取消，不再有效。
     */
    private Integer status;

    /** 订单的总金额，以系统货币为单位（例如，人民币元）。从订单项计算得出。 */
    private BigDecimal totalAmount;

    /** 订单创建的时间戳。 */
    private LocalDateTime createdAt;

    /** 订单最后更新的时间戳。 */
    private LocalDateTime updatedAt;

    /**
     * 获取主键ID。
     *
     * @return 订单的ID
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
     * 获取订单号。
     *
     * @return 唯一的订单号
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 设置订单号。
     *
     * @param orderNo 要设置的订单号
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 获取用户ID。
     *
     * @return 下单用户的ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户ID。
     *
     * @param userId 要设置的用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取订单状态。
     *
     * @return 订单的状态代码
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置订单状态。
     *
     * @param status 要设置的状态代码
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 获取总金额。
     *
     * @return 订单的总金额
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * 设置总金额。
     *
     * @param totalAmount 要设置的总金额
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 获取创建时间戳。
     *
     * @return 订单的创建时间
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
     * @return 订单的最后更新时间
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
