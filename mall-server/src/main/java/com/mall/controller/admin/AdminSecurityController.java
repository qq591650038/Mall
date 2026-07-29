package com.mall.controller.admin;

import com.mall.common.result.Result;
import com.mall.entity.Admin;
import com.mall.exception.BusinessException;
import com.mall.service.AdminService;
import com.mall.utils.PasswordUtil;
import com.mall.utils.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 管理员安全控制器。
 * 提供敏感操作二次验证相关接口，管理员需通过密码验证获取一次性Token，
 * 用于后续敏感操作的身份确认。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/security")
@Tag(name = "管理员安全验证", description = "敏感操作二次验证相关接口")
public class AdminSecurityController {

    /** 验证Token有效期（分钟） */
    private static final int VERIFY_TOKEN_EXPIRE_MINUTES = 5;

    /** Redis key 前缀 - 敏感操作验证Token */
    private static final String VERIFY_TOKEN_KEY_PREFIX = "sensitive:verify:";

    private final AdminService adminService;
    private final PasswordUtil passwordUtil;
    private final RedisUtil redisUtil;

    public AdminSecurityController(AdminService adminService,
                                    PasswordUtil passwordUtil,
                                    RedisUtil redisUtil) {
        this.adminService = adminService;
        this.passwordUtil = passwordUtil;
        this.redisUtil = redisUtil;
    }

    /**
     * 敏感操作二次验证（管理员重新输入密码获取验证Token）。
     * 验证通过后生成一次性验证Token，存入Redis，有效期5分钟。
     * 后续敏感操作需在请求头中携带 X-Verify-Token。
     */
    @PostMapping("/verify")
    @Operation(summary = "敏感操作验证", description = "管理员重新输入密码获取验证Token，用于敏感操作二次确认")
    public Result<Map<String, Object>> verify(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Long adminId) {

        if (adminId == null) {
            throw new BusinessException(com.mall.common.result.ErrorCode.UNAUTHORIZED, "请先登录");
        }

        String password = body.get("password");
        if (password == null || password.isEmpty()) {
            throw new BusinessException(com.mall.common.result.ErrorCode.BAD_REQUEST, "密码不能为空");
        }

        // 查询管理员信息
        Admin admin = adminService.findById(adminId);
        if (admin == null) {
            throw new BusinessException(com.mall.common.result.ErrorCode.USER_NOT_EXIST, "管理员不存在");
        }

        // 验证密码
        if (!passwordUtil.matches(password, admin.getPassword())) {
            log.warn("管理员敏感操作验证失败：密码错误, adminId={}", adminId);
            throw new BusinessException(com.mall.common.result.ErrorCode.PASSWORD_ERROR, "密码错误");
        }

        // 生成验证Token并存入Redis，有效期5分钟
        String verifyToken = UUID.randomUUID().toString().replace("-", "");
        String redisKey = VERIFY_TOKEN_KEY_PREFIX + adminId;
        redisUtil.set(redisKey, verifyToken, VERIFY_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("管理员敏感操作验证通过: adminId={}", adminId);

        Map<String, Object> result = new HashMap<>();
        result.put("verifyToken", verifyToken);
        result.put("expiresIn", VERIFY_TOKEN_EXPIRE_MINUTES * 60);

        return Result.success("验证成功", result);
    }

    /**
     * 检查当前管理员的验证Token是否有效。
     * 方便前端在进入敏感操作页面前轮询检查。
     */
    @GetMapping("/check")
    @Operation(summary = "检查验证Token", description = "检查当前管理员的验证Token是否有效")
    public Result<Map<String, Object>> check(
            @RequestHeader("X-Verify-Token") String verifyToken,
            @AuthenticationPrincipal Long adminId) {

        if (adminId == null) {
            throw new BusinessException(com.mall.common.result.ErrorCode.UNAUTHORIZED, "请先登录");
        }

        String redisKey = VERIFY_TOKEN_KEY_PREFIX + adminId;
        String storedToken = redisUtil.getString(redisKey);

        boolean valid = storedToken != null && storedToken.equals(verifyToken);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);

        return Result.success("检查完成", result);
    }
}