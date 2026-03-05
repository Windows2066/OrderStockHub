package com.example.project1.persistence.mapper;

import com.example.project1.persistence.model.InventoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 库存表{@code t_inventory}的MyBatis映射器接口。
 *
 * 此接口提供数据访问方法来管理库存记录。它使用MyBatis注解定义SQL查询和更新，确保并发环境中的库存管理原子操作。
 */
@Mapper
public interface InventoryMapper {

    /**
     * 通过其唯一SKU代码检索库存记录。
     *
     * 此方法查询库存表以获取特定产品变体的详细信息。它用于在下单等操作之前检查当前库存水平。
     *
     * @param skuCode 标识产品变体的唯一SKU代码
     * @return 如果找到则返回{@link InventoryEntity}，如果给定SKU不存在记录则返回{@code null}
     */
    @Select("""
        SELECT id, sku_code, available_qty, locked_qty, version, created_at, updated_at
        FROM t_inventory
        WHERE sku_code = #{skuCode}
        """)
    InventoryEntity selectBySkuCode(String skuCode);

    /**
     * 扣减可用库存以防止超卖。
     *
     * 此方法原子地减少可用数量，仅在存在足够库存时才会更新。它使用条件更新以确保操作在库存不足时失败，防止负库存水平。
     * 版本号递增用于乐观锁定。
     *
     * 典型用法：在下单时预留库存。
     *
     * @param skuCode 要从中扣减的产品唯一SKU代码
     * @param qty 从可用库存中扣减的数量
     * @return 如果扣减成功则返回1，如果库存不足或SKU不存在则返回0
     */
    @Update("""
        UPDATE t_inventory
        SET available_qty = available_qty - #{qty},
            version = version + 1
        WHERE sku_code = #{skuCode}
          AND available_qty >= #{qty}
        """)
    int deductAvailable(@Param("skuCode") String skuCode, @Param("qty") int qty);

    /**
     * 为订单取消或补偿补充可用库存。
     *
     * 此方法增加可用数量，通常用于订单取消或补偿失败操作时。它确保库存返回流通。
     * 版本号递增以维护并发场景中的一致性。
     *
     * @param skuCode 要补充的产品唯一SKU代码
     * @param qty 添加回可用库存的数量
     * @return 受影响的行数（成功时应为1，SKU未找到时为0）
     */
    @Update("""
        UPDATE t_inventory
        SET available_qty = available_qty + #{qty},
            version = version + 1
        WHERE sku_code = #{skuCode}
        """)
    int rollbackAvailable(@Param("skuCode") String skuCode, @Param("qty") int qty);
}
