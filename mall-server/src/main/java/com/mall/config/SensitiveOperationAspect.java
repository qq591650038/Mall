package com.mall.config;

import com.mall.annotation.SensitiveOperation;
import com.mall.common.result.ErrorCode;
import com.mall.exception.BusinessException;
import com.mall.utils.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 敏感操作安全验证切面。
 * 拦截所有标注了 @SensitiveOperation 的方法，校验请求头中是否携带有效的 X-Verify-Token。
 *
 * 验证流程：
 * 1. 从请求头获取 X-Verify-Token
 * 2. 从 SecurityContext 获取当前管理员ID
 * 3. 校验Redis中是否存在对应的验证Token（key: sensitive:verify:{adminId}）
 * 4. 验证通过后放行，否则抛出 BusinessException
 *
 * 注意：此切面仅对管理员操作生效。
 */
@Slf4j
@Aspect
@Component
public class SensitiveOperationAspect {

    /** 请求头中携带的验证Token名称 */
    private static final String VERIFY_TOKEN_HEADER = "X-Verify-Token";

    /** Redis key 前缀 - 敏感操作验证Token */
    private static final String VERIFY_TOKEN_KEY_PREFIX = "sensitive:verify:";

    private final RedisUtil redisUtil;

    public SensitiveOperationAspect(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 拦截所有标注了 @SensitiveOperation 的方法。
     * 切点覆盖 com.mall 包下所有带该注解的方法。
     */
    @Around("@annotation(com.mall.annotation.SensitiveOperation)")
    public Object verifySensitiveOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前请求
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无法获取请求上下文");
        }
        HttpServletRequest request = attributes.getRequest();

        // 1. 获取方法上的注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SensitiveOperation annotation = method.getAnnotation(SensitiveOperation.class);
        String operationDesc = annotation.value();

        // 2. 获取管理员ID（从当前认证上下文）
        Long adminId = getCurrentAdminId();
        if (adminId == null) {
            log.warn("敏感操作验证失败：未登录管理员，操作={}", operationDesc);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        // 3. 从请求头获取验证Token
        String verifyToken = request.getHeader(VERIFY_TOKEN_HEADER);
        if (verifyToken == null || verifyToken.isEmpty()) {
            log.warn("敏感操作验证失败：缺少验证Token，adminId={}, operation={}", adminId, operationDesc);
            throw new BusinessException(ErrorCode.FORBIDDEN, "请先完成身份验证");
        }

        // 4. 校验Redis中的验证Token
        String redisKey = VERIFY_TOKEN_KEY_PREFIX + adminId;
        String storedToken = redisUtil.getString(redisKey);
        if (storedToken == null || !storedToken.equals(verifyToken)) {
            log.warn("敏感操作验证失败：Token无效或已过期，adminId={}, operation={}", adminId, operationDesc);
            throw new BusinessException(ErrorCode.FORBIDDEN, "请先完成身份验证");
        }

        // 5. 验证通过，放行
        log.info("敏感操作验证通过: adminId={}, operation={}", adminId, operationDesc);
        return joinPoint.proceed();
    }

    /**
     * 从当前安全上下文中获取管理员ID。
     * JwtAuthenticationFilter 将adminId作为principal存入Authentication。
     */
    private Long getCurrentAdminId() {
        try {
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Long) {
                return (Long) authentication.getPrincipal();
            }
        } catch (Exception e) {
            log.warn("获取管理员ID失败: {}", e.getMessage());
        }
        return null;
    }
}