package com.example.project1.persistence.mapper;

import com.example.project1.persistence.model.OrderItemEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单项表{@code t_order_item}的MyBatis映射器接口。
 *
 * 此接口管理订单的详细行项。每个订单可以有多个项，每个项代表购买的特定SKU。这允许复杂订单的产品组成和准确的库存跟踪。
 */
@Mapper
public interface OrderItemMapper {

    /**
     * 批量插入多个订单项记录。
     *
     * 此方法在一个数据库操作中高效插入订单的所有项。它使用MyBatis动态SQL生成适当的INSERT语句。调用方必须确保所有项属于同一订单（相同orderId）且列表不为空。
     *
     * 典型用法：在创建订单后立即插入其所有行项。
     *
     * @param items 要插入的{@link OrderItemEntity}对象列表；所有项必须具有相同的orderId
     * @return 受影响的行数（成功时应等于输入列表的大小）
     */
    @Insert("""
        <script>
        INSERT INTO t_order_item (order_id, sku_code, sku_name, price, quantity)
        VALUES
        <foreach collection='items' item='item' separator=','>
            (#{item.orderId}, #{item.skuCode}, #{item.skuName}, #{item.price}, #{item.quantity})
        </foreach>
        </script>
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int batchInsert(@Param("items") List<OrderItemEntity> items);
}
