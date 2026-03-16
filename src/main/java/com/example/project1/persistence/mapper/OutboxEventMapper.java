package com.example.project1.persistence.mapper;

import com.example.project1.persistence.model.OutboxEventEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 事件表映射器。
 */
@Mapper
public interface OutboxEventMapper {

    @Insert("""
        INSERT INTO t_outbox_event
        (event_type, biz_key, topic, tags, payload, status, retry_count, next_retry_at)
        VALUES
        (#{eventType}, #{bizKey}, #{topic}, #{tags}, #{payload}, #{status}, #{retryCount}, #{nextRetryAt})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OutboxEventEntity entity);

    @Select("""
        SELECT id, event_type, biz_key, topic, tags, payload, status, retry_count, next_retry_at, last_error, created_at, updated_at
        FROM t_outbox_event
        WHERE status IN (0, 2)
          AND (next_retry_at IS NULL OR next_retry_at <= NOW())
        ORDER BY id ASC
        LIMIT #{limit}
        """)
    List<OutboxEventEntity> selectPending(@Param("limit") int limit);

    @Select("""
        SELECT id, event_type, biz_key, topic, tags, payload, status, retry_count, next_retry_at, last_error, created_at, updated_at
        FROM t_outbox_event
        WHERE event_type = #{eventType}
          AND biz_key = #{bizKey}
        LIMIT 1
        """)
    OutboxEventEntity selectByEventTypeAndBizKey(@Param("eventType") String eventType,
                                                  @Param("bizKey") String bizKey);

    @Update("""
        UPDATE t_outbox_event
        SET status = 1,
            retry_count = 0,
            next_retry_at = NULL,
            last_error = NULL,
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int markPublished(@Param("id") Long id);

    @Update("""
        UPDATE t_outbox_event
        SET status = 2,
            retry_count = retry_count + 1,
            next_retry_at = #{nextRetryAt},
            last_error = #{lastError},
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int markFailed(@Param("id") Long id,
                   @Param("nextRetryAt") LocalDateTime nextRetryAt,
                   @Param("lastError") String lastError);
}
