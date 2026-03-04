package com.example.project1.persistence.model;

import java.time.LocalDateTime;

/**
 * 电商系统中库存修改的日志实体，用于跟踪所有库存变更。
 * 此实体对应数据库中的{@code t_inventory_log}表。它作为每次库存修改的审计跟踪，
 * 启用可追溯性、调试和补偿机制。每个日志条目捕获修改前后的库存状态以及元数据。
 * 使用场景：
 * - 审计库存交易以符合要求。
 * - 排除库存水平差异。
 * - 实施失败操作的补偿逻辑（例如，回滚）。
 * 日志一旦创建即不可变，确保可靠的历史记录。
 */
public class InventoryLogEntity {

    /** 数据库主键。自动生成的日志条目唯一标识符。 */
    private Long id;

    /** 业务关联号，例如订单号或请求幂等键，将修改链接到特定业务操作。 */
    private String bizNo;

    /** 发生库存修改的产品变体SKU代码。 */
    private String skuCode;

    /** 修改类型，例如“DEDUCT”表示库存减少或“ROLLBACK”表示撤销之前的修改。 */
    private String changeType;

    /** 此操作中修改的数量（增加为正，减少为负）。 */
    private Integer changeQty;

    /** 修改前的库存数量快照。 */
    private Integer beforeQty;

    /** 修改后的库存数量快照。 */
    private Integer afterQty;

    /** 关于修改的附加备注或上下文信息。 */
    private String remark;

    /** 日志条目创建的时间戳。 */
    private LocalDateTime createdAt;

    /**
     * 获取主键ID。
     *
     * @return 日志条目的ID
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
     * 获取业务关联号。
     *
     * @return 链接到操作的业务号
     */
    public String getBizNo() {
        return bizNo;
    }

    /**
     * 设置业务关联号。
     *
     * @param bizNo 要设置的业务号
     */
    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    /**
     * 获取SKU代码。
     *
     * @return 修改项的SKU代码
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
     * 获取修改类型。
     *
     * @return 库存修改的类型
     */
    public String getChangeType() {
        return changeType;
    }

    /**
     * 设置修改类型。
     *
     * @param changeType 要设置的修改类型
     */
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    /**
     * 获取修改的数量。
     *
     * @return 此操作中修改的数量
     */
    public Integer getChangeQty() {
        return changeQty;
    }

    /**
     * 设置修改的数量。
     *
     * @param changeQty 要设置的修改数量
     */
    public void setChangeQty(Integer changeQty) {
        this.changeQty = changeQty;
    }

    /**
     * 获取修改前的库存数量。
     *
     * @return 修改前的数量快照
     */
    public Integer getBeforeQty() {
        return beforeQty;
    }

    /**
     * 设置修改前的库存数量。
     *
     * @param beforeQty 要设置的修改前数量
     */
    public void setBeforeQty(Integer beforeQty) {
        this.beforeQty = beforeQty;
    }

    /**
     * 获取修改后的库存数量。
     *
     * @return 修改后的数量快照
     */
    public Integer getAfterQty() {
        return afterQty;
    }

    /**
     * 设置修改后的库存数量。
     *
     * @param afterQty 要设置的修改后数量
     */
    public void setAfterQty(Integer afterQty) {
        this.afterQty = afterQty;
    }

    /**
     * 获取备注。
     *
     * @return 修改的附加备注
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置备注。
     *
     * @param remark 要设置的备注
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取创建时间戳。
     *
     * @return 日志条目的创建时间
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
}
