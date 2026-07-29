package com.mall.service.impl;

import com.mall.service.LoginRateLimitService;
import com.mall.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录限流服务实现类。
 * 基于Redis实现账户+IP双重维度的失败计数和锁定机制。
 *
 * 限流规则：
 * - 5次失败后锁定30分钟
 * - Redis key: login:fail:{account}（账户维度）、login:ip:{ip}（IP维度）
 * - 任一维度达到5次即触发锁定
 * - 所有异常均不影响登录主流程（try-catch包裹，限流失败时放行）
 */
@Slf4j
@Service
public class LoginRateLimitServiceImpl implements LoginRateLimitService {

    /** 最大失败次数阈值，达到后触发锁定 */
    private static final int MAX_FAIL_ATTEMPTS = 5;

    /** 锁定时长（分钟） */
    private static final int LOCK_MINUTES = 30;

    /** Redis key 前缀 - 账户维度失败计数 */
    private static final String KEY_PREFIX_ACCOUNT = "login:fail:";

    /** Redis key 前缀 - IP维度失败计数 */
    private static final String KEY_PREFIX_IP = "login:ip:";

    private final RedisUtil redisUtil;

    public LoginRateLimitServiceImpl(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public void recordFailedAttempt(String account, String ip) {
        try {
            // 账户维度累加
            String accountKey = KEY_PREFIX_ACCOUNT + account;
            Long accountCount = redisUtil.increment(accountKey, 1);
            // 设置TTL为锁定时长（每次失败刷新TTL，实现滑动窗口效果）
            redisUtil.expire(accountKey, LOCK_MINUTES, TimeUnit.MINUTES);

            // IP维度累加
            String ipKey = KEY_PREFIX_IP + ip;
            Long ipCount = redisUtil.increment(ipKey, 1);
            redisUtil.expire(ipKey, LOCK_MINUTES, TimeUnit.MINUTES);

            log.warn("登录失败计数: account={}, ip={}, accountCount={}, ipCount={}",
                    account, ip, accountCount, ipCount);
        } catch (Exception e) {
            // 限流异常不影响主流程
            log.error("记录登录失败尝试异常，放行: account={}, ip={}, error={}", account, ip, e.getMessage());
        }
    }

    @Override
    public void recordSuccess(String account, String ip) {
        try {
            // 清除账户维度的失败记录
            redisUtil.delete(KEY_PREFIX_ACCOUNT + account);
            // 清除IP维度的失败记录
            redisUtil.delete(KEY_PREFIX_IP + ip);
            log.info("登录成功，已清除限流记录: account={}, ip={}", account, ip);
        } catch (Exception e) {
            // 清除异常不影响主流程
            log.error("清除限流记录异常: account={}, ip={}, error={}", account, ip, e.getMessage());
        }
    }

    @Override
    public boolean isLocked(String account, String ip) {
        try {
            // 检查账户维度
            String accountKey = KEY_PREFIX_ACCOUNT + account;
            Object accountCountObj = redisUtil.get(accountKey);
            if (accountCountObj != null) {
                int accountCount = Integer.parseInt(accountCountObj.toString());
                if (accountCount >= MAX_FAIL_ATTEMPTS) {
                    log.warn("账户维度触发锁定: account={}, count={}", account, accountCount);
                    return true;
                }
            }

            // 检查IP维度
            String ipKey = KEY_PREFIX_IP + ip;
            Object ipCountObj = redisUtil.get(ipKey);
            if (ipCountObj != null) {
                int ipCount = Integer.parseInt(ipCountObj.toString());
                if (ipCount >= MAX_FAIL_ATTEMPTS) {
                    log.warn("IP维度触发锁定: ip={}, count={}", ip, ipCount);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            // 限流检查异常时放行，不阻塞登录
            log.error("检查锁定状态异常，放行: account={}, ip={}, error={}", account, ip, e.getMessage());
            return false;
        }
    }

    @Override
    public long getRemainingLockTime(String account, String ip) {
        try {
            long accountRemaining = getKeyTtl(KEY_PREFIX_ACCOUNT + account);
            long ipRemaining = getKeyTtl(KEY_PREFIX_IP + ip);
            // 返回两者中较大的剩余时间
            return Math.max(accountRemaining, ipRemaining);
        } catch (Exception e) {
            log.error("获取剩余锁定时间异常: account={}, ip={}, error={}", account, ip, e.getMessage());
            return 0;
        }
    }

    /**
     * 获取Redis key的剩余TTL（秒）。
     * key不存在或无过期时间返回0。
     */
    private long getKeyTtl(String key) {
        try {
            Long ttl = redisUtil.getExpire(key);
            if (ttl != null && ttl > 0) {
                return ttl;
            }
            // ttl == -1 表示永不过期，ttl == -2 表示key不存在
        } catch (Exception e) {
            log.warn("获取TTL异常: key={}, error={}", key, e.getMessage());
        }
        return 0;
    }
}