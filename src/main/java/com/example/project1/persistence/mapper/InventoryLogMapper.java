package com.example.project1.persistence.mapper;

import com.example.project1.persistence.model.InventoryLogEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * 库存日志表{@code t_inventory_log}的MyBatis映射器接口。
 *
 * 此接口处理所有库存变更的日志记录，以实现审计和可追溯性目的。每次库存修改（扣减、补充等）都应在此记录，以维护库存变动的完整历史。
 */
@Mapper
public interface InventoryLogMapper {

    /**
     * 插入新的库存变更日志条目。
     *
     * 此方法记录单个库存修改事件，捕获所有相关细节，包括业务上下文、受影响的SKU、变更类型、变更前后的数量以及任何附加备注。
     * 日志条目一旦创建即不可变。
     *
     * 在任何库存更新后使用此方法以确保可审计性和调试能力。
     *
     * @param log 包含日志细节的{@link InventoryLogEntity}，包括业务号、SKU代码、变更类型、数量和备注
     * @return 受影响的行数（成功插入时应为1）
     */
    @Insert("""
        INSERT INTO t_inventory_log
        (biz_no, sku_code, change_type, change_qty, before_qty, after_qty, remark)
        VALUES
        (#{bizNo}, #{skuCode}, #{changeType}, #{changeQty}, #{beforeQty}, #{afterQty}, #{remark})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InventoryLogEntity log);
}
