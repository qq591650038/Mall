package com.mall.controller.admin;

import com.mall.common.result.Result;
import com.mall.dto.admin.AdminLoginDTO;
import com.mall.service.AdminService;
import com.mall.service.LoginRateLimitService;
import com.mall.utils.IpUtil;
import com.mall.utils.RedisUtil;
import com.mall.vo.admin.AdminLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "管理员认证", description = "管理员登录、退出等接口")
public class AdminAuthController {

    private final AdminService adminService;
    private final RedisUtil redisUtil;
    private final LoginRateLimitService loginRateLimitService;
    private final Random random = new Random();

    public AdminAuthController(AdminService adminService,
                               RedisUtil redisUtil,
                               LoginRateLimitService loginRateLimitService) {
        this.adminService = adminService;
        this.redisUtil = redisUtil;
        this.loginRateLimitService = loginRateLimitService;
    }

    @PostConstruct
    public void init() {
        adminService.initializeDefaultAdmin();
    }

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "管理员通过用户名+密码登录，返回JWT Token")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO loginDTO,
                                      HttpServletRequest request) {
        String account = loginDTO.getUsername();
        String ip = IpUtil.getClientIp(request);

        // 1. 登录前检查限流状态
        try {
            if (loginRateLimitService.isLocked(account, ip)) {
                long remainingSeconds = loginRateLimitService.getRemainingLockTime(account, ip);
                long remainingMinutes = (remainingSeconds + 59) / 60;
                return Result.error(423,
                        "账号或IP已被临时锁定，请" + remainingMinutes + "分钟后再试");
            }
        } catch (Exception e) {
            // 限流检查异常不阻塞登录主流程
            log.warn("管理员登录限流检查异常，放行: account={}, ip={}", account, ip);
        }

        // 2. 执行登录
        try {
            AdminLoginVO loginVO = adminService.login(loginDTO);
            // 登录成功，清除限流记录
            try {
                loginRateLimitService.recordSuccess(account, ip);
            } catch (Exception e) {
                log.warn("管理员登录清除限流记录异常: account={}, ip={}", account, ip);
            }
            return Result.success("登录成功", loginVO);
        } catch (Exception e) {
            // 登录失败，记录失败次数
            try {
                loginRateLimitService.recordFailedAttempt(account, ip);
                // 检查是否达到锁定阈值
                if (loginRateLimitService.isLocked(account, ip)) {
                    long remainingSeconds = loginRateLimitService.getRemainingLockTime(account, ip);
                    long remainingMinutes = (remainingSeconds + 59) / 60;
                    log.warn("管理员登录失败达到锁定阈值: account={}, ip={}", account, ip);
                    return Result.error(423,
                            "登录失败次数过多，账号已被锁定，请" + remainingMinutes + "分钟后再试");
                }
            } catch (Exception ex) {
                log.warn("管理员登录记录失败异常: account={}, ip={}", account, ip);
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "管理员退出登录", description = "管理员退出登录，清除Redis中的登录状态")
    public Result<Void> logout(@AuthenticationPrincipal Long adminId) {
        if (adminId != null) {
            adminService.logout(adminId);
        }
        return Result.success("退出成功", null);
    }

    @GetMapping("/verify-code")
    @Operation(summary = "获取验证码", description = "获取验证码key，验证码存入Redis")
    public Result<Map<String, String>> getVerifyCode() {
        String code = String.format("%04d", random.nextInt(10000));
        String key = "verify:admin:" + System.currentTimeMillis() + ":" + random.nextInt(1000);

        redisUtil.set(key, code, 5, TimeUnit.MINUTES);

        Map<String, String> result = new HashMap<>();
        result.put("key", key);
        return Result.success("获取验证码成功", result);
    }
}