package com.mall.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
public class RocketMqConfig {
    @Bean(destroyMethod = "shutdown")
    public DefaultMQProducer seckillProducer(@Value("${rocketmq.name-server}") String nameServer,
                                             @Value("${rocketmq.topic}") String topic) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("mall-seckill-producer");
        producer.setNamesrvAddr(nameServer);
        producer.setCreateTopicKey(topic);
        producer.start();
        return producer;
    }
}
