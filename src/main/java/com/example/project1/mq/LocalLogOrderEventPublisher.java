package com.example.project1.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 本地日志发布器。
 *
 * 当 RocketMQ 未启用时，使用该发布器模拟成功发布，便于本地先跑通 Outbox 流程。
 */
@Component
@ConditionalOnMissingBean(OrderEventPublisher.class)
public class LocalLogOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LocalLogOrderEventPublisher.class);

    @Override
    public boolean publish(String topic, String tags, String key, String payload) {
        log.info("本地模拟发布消息，topic={}, tags={}, key={}, payload={}", topic, tags, key, payload);
        return true;
    }
}

