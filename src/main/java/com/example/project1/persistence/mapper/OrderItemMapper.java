package com.example.project1.persistence.mapper;

import com.example.project1.persistence.model.OrderItemEntity;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单项表{@code t_order_item}的MyBatis映射器接口。
 * <p>
 * 此接口管理订单的详细行项。每个订单可以有多个项，每个项代表购买的特定SKU。这允许复杂订单的产品组成和准确的库存跟踪。
 */
@Mapper
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection", "SqlDialectInspection"})
public interface OrderItemMapper {

    /**
     * 批量插入多个订单项记录。
     * <p>
     * 此方法在一个数据库操作中高效插入订单的所有项。它使用MyBatis动态SQL生成适当的INSERT语句。调用方必须确保所有项属于同一订单（相同orderId）且列表不为空。
     *
     * 典型用法：在创建订单后立即插入其所有行项。
     *
     * @param items 要插入的{@link OrderItemEntity}对象列表；所有项必须具有相同的orderId
     * @return 受影响的行数（成功时应等于输入列表的大小）
     */
    @InsertProvider(type = OrderItemSqlProvider.class, method = "batchInsert")
    int batchInsert(@Param("items") List<OrderItemEntity> items);

    /**
     * 按订单ID查询订单明细列表。
     *
     * @param orderId 订单主键ID
     * @return 订单明细列表
     */
    @Select("""
        SELECT id, order_id, sku_code, sku_name, price, quantity, created_at, updated_at
        FROM t_order_item
        WHERE order_id = #{orderId}
        ORDER BY id ASC
        """)
    List<OrderItemEntity> selectByOrderId(Long orderId);

    /**
     * 订单项 SQL 生成器：用于构建批量插入语句，避免注解中使用 XML 脚本标签导致 IDE 误报。
     */
    class OrderItemSqlProvider {

        /**
         * 构建批量插入 SQL。
         *
         * @param params MyBatis 传入参数，包含 `items` 列表
         * @return 可被 MyBatis 直接执行的 INSERT 语句
         */
        @SuppressWarnings("unchecked")
        public String batchInsert(Map<String, Object> params) {
            List<OrderItemEntity> items = (List<OrderItemEntity>) params.get("items");
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO t_order_item (order_id, sku_code, sku_name, price, quantity) VALUES ");

            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    sql.append(",");
                }
                sql.append("(#{items[")
                    .append(i)
                    .append("].orderId}, #{items[")
                    .append(i)
                    .append("].skuCode}, #{items[")
                    .append(i)
                    .append("].skuName}, #{items[")
                    .append(i)
                    .append("].price}, #{items[")
                    .append(i)
                    .append("].quantity})");
            }
            return sql.toString();
        }
    }
}
