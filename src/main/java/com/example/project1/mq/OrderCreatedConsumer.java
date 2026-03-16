package com.example.project1.mq;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 订单创建事件消费者。
 *
 * 这里先做最小可运行消费：收到消息后打印日志。
 * 后续可扩展为发券、积分、通知等异步动作。
 */
@Component
@ConditionalOnProperty(value = "app.rocketmq.enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${app.rocketmq.order-topic}",
        consumerGroup = "${app.rocketmq.consumer-group}"
)
public class OrderCreatedConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    @Override
    public void onMessage(String message) {
        log.info("收到订单创建事件：{}", message);
    }
}

