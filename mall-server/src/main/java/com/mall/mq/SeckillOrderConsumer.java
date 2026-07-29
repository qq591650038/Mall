package com.mall.mq;

import com.mall.service.SeckillAsyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
public class SeckillOrderConsumer implements SmartLifecycle {
    private final SeckillAsyncService seckillAsyncService;
    private final String nameServer;
    private final String topic;
    private final String group;
    private volatile boolean running;
    private DefaultMQPushConsumer consumer;

    public SeckillOrderConsumer(SeckillAsyncService seckillAsyncService,
                                @Value("${rocketmq.name-server}") String nameServer,
                                @Value("${rocketmq.topic}") String topic,
                                @Value("${rocketmq.consumer-group}") String group) {
        this.seckillAsyncService = seckillAsyncService;
        this.nameServer = nameServer;
        this.topic = topic;
        this.group = group;
    }

    @Override
    public void start() {
        if (running) return;
        try {
            consumer = new DefaultMQPushConsumer(group);
            consumer.setNamesrvAddr(nameServer);
            consumer.setConsumeThreadMin(4);
            consumer.setConsumeThreadMax(16);
            consumer.subscribe(topic, "*");
            consumer.registerMessageListener((MessageListenerConcurrently) this::consume);
            consumer.start();
            running = true;
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ seckill consumer failed to start", e);
        }
    }

    private ConsumeConcurrentlyStatus consume(List<MessageExt> messages, ConsumeConcurrentlyContext context) {
        for (MessageExt message : messages) {
            String requestId = new String(message.getBody(), StandardCharsets.UTF_8);
            try {
                seckillAsyncService.process(requestId);
            } catch (Exception e) {
                log.warn("Seckill order failed: requestId={}", requestId, e);
                seckillAsyncService.failAndCompensate(requestId, e.getMessage());
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    @Override
    public void stop() {
        if (consumer != null) consumer.shutdown();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
