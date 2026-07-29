package com.mall.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            meterRegistry.counter("mall.redis.errors", "operation", "set").increment();
            log.error("Redis set error: {}", e.getMessage());
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            meterRegistry.counter("mall.redis.errors", "operation", "get").increment();
            log.error("Redis set with timeout error: {}", e.getMessage());
        }
    }

    public void set(String key, Object value, Duration duration) {
        try {
            redisTemplate.opsForValue().set(key, value, duration);
        } catch (Exception e) {
            meterRegistry.counter("mall.redis.errors", "operation", "delete").increment();
            log.error("Redis set with duration error: {}", e.getMessage());
        }
    }

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            meterRegistry.counter("mall.redis.errors", "operation", "increment").increment();
            log.error("Redis get error: {}", e.getMessage());
            return null;
        }
    }

    public String getString(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            meterRegistry.counter("mall.redis.errors", "operation", "decrement").increment();
            log.error("Redis getString error: {}", e.getMessage());
            return null;
        }
    }

    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis delete error: {}", e.getMessage());
            return false;
        }
    }

    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis hasKey error: {}", e.getMessage());
            return false;
        }
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis expire error: {}", e.getMessage());
            return false;
        }
    }

    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis increment error: {}", e.getMessage());
            return null;
        }
    }

    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("Redis decrement error: {}", e.getMessage());
            return null;
        }
    }

    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        try { return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit); }
        catch (Exception e) { meterRegistry.counter("mall.redis.errors", "operation", "setIfAbsent").increment(); log.error("Redis setIfAbsent error: {}", e.getMessage()); return false; }
    }

    /**
     * 获取key的剩余过期时间（秒）。
     * @return 剩余秒数，-1表示永不过期，-2表示key不存在，异常返回-2
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis getExpire error: {}", e.getMessage());
            return -2L;
        }
    }
}
