package com.example.project1.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 基于 RocketMQ 的订单事件发布器。
 */
@Component
@ConditionalOnProperty(value = "app.rocketmq.enabled", havingValue = "true")
public class RocketMqOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMqOrderEventPublisher.class);

    private final RocketMQTemplate rocketMQTemplate;

    public RocketMqOrderEventPublisher(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public boolean publish(String topic, String tags, String key, String payload) {
        String destination = (tags == null || tags.isBlank()) ? topic : topic + ":" + tags;
        rocketMQTemplate.syncSend(destination,
                MessageBuilder.withPayload(payload)
                        .setHeader("KEYS", key)
                        .build(),
                3000);
        log.info("RocketMQ 发布成功，destination={}, key={}", destination, key);
        return true;
    }
}

