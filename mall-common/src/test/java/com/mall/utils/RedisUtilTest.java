package com.mall.utils;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RedisUtilTest {

    @Test
    void redisReadFailureFallsBackToNullAndRecordsMetric() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        when(values.get("product:1")).thenThrow(new IllegalStateException("redis unavailable"));
        when(values.increment("stock:1", 1)).thenThrow(new IllegalStateException("redis unavailable"));
        when(values.setIfAbsent("lock:1", "token", 10, java.util.concurrent.TimeUnit.SECONDS))
                .thenThrow(new IllegalStateException("redis unavailable"));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RedisUtil redisUtil = new RedisUtil(template, registry);

        assertNull(redisUtil.get("product:1"));
        assertFalse(redisUtil.hasKey("product:1"));
        assertNull(redisUtil.increment("stock:1", 1));
        assertFalse(redisUtil.setIfAbsent("lock:1", "token", 10, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(1.0, registry.counter("mall.redis.errors", "operation", "increment").count());
    }
}
