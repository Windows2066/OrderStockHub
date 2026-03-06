package com.example.project1.persistence.mapper;

import com.example.project1.persistence.model.OrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * 订单主表{@code t_order}的MyBatis映射器接口。
 * <p>
 * 此接口提供数据访问操作来管理订单记录。它处理订单的创建和检索，这是电商工作流的核心。订单链接到订单项并触发库存操作。
 */
@Mapper
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public interface OrderMapper {

    /**
     * 将新订单记录插入数据库。
     * <p>
     * 此方法创建包含基本细节的主订单条目。订单号应提前生成并唯一。时间戳由数据库默认处理。
     *
     * @param order 包含订单细节的{@link OrderEntity}，如订单号、用户ID、状态和总金额
     * @return 受影响的行数（成功插入时应为1）
     */
    // noinspection SqlResolve
    @Insert("""
        INSERT INTO t_order (order_no, request_id, user_id, status, total_amount)
        VALUES (#{orderNo}, #{requestId}, #{userId}, #{status}, #{totalAmount})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderEntity order);

    /**
     * 通过其主键ID检索订单。
     * <p>
     * 此方法获取完整的订单记录，用于状态更新或显示订单细节等操作。
     *
     * @param id 订单的主键ID
     * @return 如果找到则返回{@link OrderEntity}，如果给定ID不存在订单则返回{@code null}
     */
    // noinspection SqlResolve
    @Select("""
        SELECT id, order_no, request_id, user_id, status, total_amount, created_at, updated_at
        FROM t_order
        WHERE id = #{id}
        """)
    OrderEntity selectById(Long id);

    /**
     * 通过其业务订单号检索订单。
     *
     * 此方法用于幂等操作和跟踪，因为订单号在整个系统中全局唯一，通常在外部系统或用户通信中引用。
     *
     * @param orderNo 唯一的业务订单号
     * @return 如果找到则返回{@link OrderEntity}，如果给定订单号不存在订单则返回{@code null}
     */
    // noinspection SqlResolve
    @Select("""
        SELECT id, order_no, request_id, user_id, status, total_amount, created_at, updated_at
        FROM t_order
        WHERE order_no = #{orderNo}
        """)
    OrderEntity selectByOrderNo(String orderNo);

    /**
     * 通过请求幂等号查询订单。
     *
     * @param requestId 客户端请求幂等号
     * @return 对应订单，不存在则返回{@code null}
     */
    // noinspection SqlResolve
    @Select("""
        SELECT id, order_no, request_id, user_id, status, total_amount, created_at, updated_at
        FROM t_order
        WHERE request_id = #{requestId}
        """)
    OrderEntity selectByRequestId(String requestId);
}
