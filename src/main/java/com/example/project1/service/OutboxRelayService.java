package com.example.project1.service;

import com.example.project1.mq.OrderEventPublisher;
import com.example.project1.persistence.mapper.OutboxEventMapper;
import com.example.project1.persistence.model.OutboxEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 转发任务。
 *
 * 定时扫描待发送事件，成功后标记为已发布，失败则增加重试次数并设置下次重试时间。
 */
@Service
@ConditionalOnProperty(value = "app.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxEventMapper outboxEventMapper;
    private final OrderEventPublisher orderEventPublisher;

    private final int relayBatchSize;

    public OutboxRelayService(OutboxEventMapper outboxEventMapper,
                              OrderEventPublisher orderEventPublisher,
                              org.springframework.core.env.Environment environment) {
        this.outboxEventMapper = outboxEventMapper;
        this.orderEventPublisher = orderEventPublisher;
        this.relayBatchSize = Integer.parseInt(environment.getProperty("app.outbox.relay-batch-size", "20"));
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-fixed-delay-ms:5000}")
    public void relayPendingEvents() {
        long startedAtNanos = System.nanoTime();
        List<OutboxEventEntity> events = outboxEventMapper.selectPending(relayBatchSize);
        if (events.isEmpty()) {
            return;
        }

        List<Long> publishedIds = new ArrayList<>(events.size());
        int failedCount = 0;

        for (OutboxEventEntity event : events) {
            try {
                boolean published = orderEventPublisher.publish(event.getTopic(), event.getTags(), event.getBizKey(), event.getPayload());
                if (published) {
                    publishedIds.add(event.getId());
                } else {
                    markFailed(event, "发布器返回失败");
                    failedCount++;
                }
            } catch (Exception ex) {
                markFailed(event, ex.getMessage());
                failedCount++;
            }
        }

        if (!publishedIds.isEmpty()) {
            try {
                outboxEventMapper.markPublishedBatch(publishedIds);
            } catch (Exception ex) {
                // 批量更新异常时退化为逐条更新，避免已发送消息长期处于待发送状态。
                log.warn("Outbox 批量更新已发送状态失败，回退逐条更新，count={}, reason={}", publishedIds.size(), ex.getMessage());
                for (Long id : publishedIds) {
                    outboxEventMapper.markPublished(id);
                }
            }
        }

        long elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
        double throughput = elapsedMs > 0 ? (events.size() * 1000.0) / elapsedMs : events.size();
        log.info("Outbox 扫描完成，total={}, success={}, failed={}, elapsedMs={}, throughput={}",
                events.size(), publishedIds.size(), failedCount, elapsedMs, String.format("%.2f", throughput));
    }

    private void markFailed(OutboxEventEntity event, String message) {
        LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(30L * (event.getRetryCount() + 1));
        outboxEventMapper.markFailed(event.getId(), nextRetryAt, truncate(message));
        log.warn("Outbox 发送失败，eventId={}, bizKey={}, reason={}", event.getId(), event.getBizKey(), message);
    }

    private String truncate(String message) {
        if (message == null) {
            return "未知异常";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
