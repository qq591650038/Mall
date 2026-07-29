package com.mall.service;

/**
 * 登录限流服务接口。
 * 支持账户维度和IP维度的双重限流，5次失败后锁定30分钟。
 */
public interface LoginRateLimitService {

    /**
     * 记录一次登录失败尝试。
     * 基于账号+IP两个维度分别累加失败计数，任一维度达到5次即触发锁定。
     *
     * @param account 登录账号（用户名/手机号/邮箱）
     * @param ip      客户端IP
     */
    void recordFailedAttempt(String account, String ip);

    /**
     * 登录成功后清除该账号和IP的失败记录，解除锁定。
     *
     * @param account 登录账号
     * @param ip      客户端IP
     */
    void recordSuccess(String account, String ip);

    /**
     * 检查账号或IP是否已被锁定。
     * 只要账户维度或IP维度任一达到5次失败，即视为锁定。
     *
     * @param account 登录账号
     * @param ip      客户端IP
     * @return true-已锁定，false-未锁定
     */
    boolean isLocked(String account, String ip);

    /**
     * 获取剩余锁定时间（秒）。
     * 返回账户维度和IP维度中较大的剩余时间。
     *
     * @param account 登录账号
     * @param ip      客户端IP
     * @return 剩余锁定秒数，未锁定返回0
     */
    long getRemainingLockTime(String account, String ip);
}