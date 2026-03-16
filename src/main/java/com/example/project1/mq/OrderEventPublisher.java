package com.example.project1.mq;

/**
 * 订单事件发布器抽象。
 */
public interface OrderEventPublisher {

    /**
     * 发布消息。
     *
     * @return true 表示发布成功
     */
    boolean publish(String topic, String tags, String key, String payload);
}

