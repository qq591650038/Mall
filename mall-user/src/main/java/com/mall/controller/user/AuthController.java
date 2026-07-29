package com.mall.controller.user;

import com.mall.common.result.Result;
import com.mall.dto.LoginDTO;
import com.mall.dto.RegisterDTO;
import com.mall.service.LoginRateLimitService;
import com.mall.service.UserService;
import com.mall.utils.IpUtil;
import com.mall.utils.RedisUtil;
import com.mall.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/auth")
@Tag(name = "用户认证", description = "用户注册、登录、退出登录等接口")
public class AuthController {

    private final UserService userService;
    private final RedisUtil redisUtil;
    private final LoginRateLimitService loginRateLimitService;
    private final Random random = new Random();

    public AuthController(UserService userService,
                          RedisUtil redisUtil,
                          LoginRateLimitService loginRateLimitService) {
        this.userService = userService;
        this.redisUtil = redisUtil;
        this.loginRateLimitService = loginRateLimitService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户通过用户名+密码+验证码进行注册")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户通过账号+密码进行登录，返回JWT Token")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO,
                                 HttpServletRequest request) {
        String account = loginDTO.getAccount();
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
            log.warn("限流检查异常，放行登录: account={}, ip={}", account, ip);
        }

        // 2. 执行登录
        try {
            LoginVO loginVO = userService.login(loginDTO);
            // 登录成功，清除限流记录
            try {
                loginRateLimitService.recordSuccess(account, ip);
            } catch (Exception e) {
                log.warn("清除限流记录异常: account={}, ip={}", account, ip);
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
                    log.warn("登录失败达到锁定阈值: account={}, ip={}", account, ip);
                    return Result.error(423,
                            "登录失败次数过多，账号已被锁定，请" + remainingMinutes + "分钟后再试");
                }
            } catch (Exception ex) {
                log.warn("记录登录失败异常: account={}, ip={}", account, ip);
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "用户退出登录，清除Redis中的登录状态")
    public Result<Void> logout(@AuthenticationPrincipal Long userId) {
        if (userId != null) {
            userService.logout(userId);
        }
        return Result.success("退出成功", null);
    }

    @GetMapping("/verify-code")
    @Operation(summary = "获取验证码", description = "获取图形验证码key，验证码存入Redis")
    public Result<Map<String, String>> getVerifyCode() {
        String code = String.format("%04d", random.nextInt(10000));
        String key = "verify:" + System.currentTimeMillis() + ":" + random.nextInt(1000);

        redisUtil.set(key, code, 5, TimeUnit.MINUTES);

        Map<String, String> result = new HashMap<>();
        result.put("key", key);
        return Result.success("获取验证码成功", result);
    }
}