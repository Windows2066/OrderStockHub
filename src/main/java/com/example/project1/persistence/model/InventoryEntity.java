package com.example.project1.persistence.model;

import java.time.LocalDateTime;

/**
 * 电商系统中产品库存的实体类。
 * 此实体对应数据库中的{@code t_inventory}表。它按SKU（库存单位）维度维护产品库存信息，
 * 该SKU是每个产品变体的唯一标识符。该实体跟踪可用数量和锁定数量，以支持订单处理并防止超卖。
 * 关键概念：
 * - 可用数量：可立即销售的库存。
 * - 锁定数量：为待处理订单或其他流程保留的库存，不适用于新销售。
 * - 版本：用于乐观锁定，以安全处理并发更新。
 * 此设计确保多线程或分布式环境中的数据一致性。
 */
public class InventoryEntity {

    /** 数据库主键。自动生成的库存记录唯一标识符。 */
    private Long id;

    /** 标识产品变体的唯一SKU代码。 */
    private String skuCode;

    /** 可销售的可用库存数量。 */
    private Integer availableQty;

    /** 为待处理订单或流程保留的锁定库存数量。 */
    private Integer lockedQty;

    /** 乐观锁定版本号，用于防止并发修改问题。 */
    private Integer version;

    /** 库存记录创建的时间戳。 */
    private LocalDateTime createdAt;

    /** 库存记录最后更新的时间戳。 */
    private LocalDateTime updatedAt;

    /**
     * 获取主键ID。
     *
     * @return 库存记录的ID
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
     * 获取SKU代码。
     *
     * @return 唯一的SKU代码
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
     * 获取可用数量。
     *
     * @return 可用的库存数量
     */
    public Integer getAvailableQty() {
        return availableQty;
    }

    /**
     * 设置可用数量。
     *
     * @param availableQty 要设置的可用数量
     */
    public void setAvailableQty(Integer availableQty) {
        this.availableQty = availableQty;
    }

    /**
     * 获取锁定数量。
     *
     * @return 锁定的库存数量
     */
    public Integer getLockedQty() {
        return lockedQty;
    }

    /**
     * 设置锁定数量。
     *
     * @param lockedQty 要设置的锁定数量
     */
    public void setLockedQty(Integer lockedQty) {
        this.lockedQty = lockedQty;
    }

    /**
     * 获取乐观锁定的版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置乐观锁定的版本号。
     *
     * @param version 要设置的版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * 获取创建时间戳。
     *
     * @return 创建时间
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
     * @return 最后更新时间
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
