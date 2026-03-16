package com.example.project1.unit.service;

import com.example.project1.mq.OrderEventPublisher;
import com.example.project1.persistence.mapper.OutboxEventMapper;
import com.example.project1.persistence.model.OutboxEventEntity;
import com.example.project1.service.OutboxRelayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Outbox 转发任务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventMapper outboxEventMapper;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private Environment environment;

    @Test
    void shouldMarkPublishedWhenPublishSuccess() {
        when(environment.getProperty("app.outbox.relay-batch-size", "20")).thenReturn("20");

        OutboxEventEntity event = buildEvent(1L, "BIZ-OK", 0);
        when(outboxEventMapper.selectPending(20)).thenReturn(List.of(event));
        when(orderEventPublisher.publish(event.getTopic(), event.getTags(), event.getBizKey(), event.getPayload())).thenReturn(true);

        OutboxRelayService relayService = new OutboxRelayService(outboxEventMapper, orderEventPublisher, environment);
        relayService.relayPendingEvents();

        verify(outboxEventMapper).markPublished(1L);
    }

    @Test
    void shouldMarkFailedWhenPublishThrowsException() {
        when(environment.getProperty("app.outbox.relay-batch-size", "20")).thenReturn("20");

        OutboxEventEntity event = buildEvent(2L, "BIZ-FAIL", 1);
        when(outboxEventMapper.selectPending(20)).thenReturn(List.of(event));
        when(orderEventPublisher.publish(event.getTopic(), event.getTags(), event.getBizKey(), event.getPayload()))
                .thenThrow(new RuntimeException("mq unavailable"));

        OutboxRelayService relayService = new OutboxRelayService(outboxEventMapper, orderEventPublisher, environment);
        relayService.relayPendingEvents();

        verify(outboxEventMapper).markFailed(ArgumentMatchers.eq(2L), ArgumentMatchers.any(LocalDateTime.class), ArgumentMatchers.contains("mq unavailable"));
    }

    private OutboxEventEntity buildEvent(Long id, String bizKey, int retryCount) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(id);
        event.setEventType("ORDER_CREATED");
        event.setBizKey(bizKey);
        event.setTopic("order-created-topic");
        event.setTags("created");
        event.setPayload("{\"orderNo\":\"" + bizKey + "\"}");
        event.setRetryCount(retryCount);
        return event;
    }
}

