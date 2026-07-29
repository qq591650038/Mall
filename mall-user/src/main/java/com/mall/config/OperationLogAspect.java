package com.mall.config;

import com.mall.entity.OperationLog;
import com.mall.mapper.OperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 管理后台操作审计切面。
 *
 * 切面放在用户/管理模块，因为操作日志实体和 Mapper 由该模块维护；
 * 切点覆盖所有业务模块的管理端 Controller，登录接口单独排除。
 */
@Aspect
@Component
public class OperationLogAspect {

    private final OperationLogMapper mapper;

    public OperationLogAspect(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    @Around("execution(* com.mall.controller.admin..*(..)) "
            + "&& !execution(* com.mall.controller.admin.AdminAuthController.login(..))")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLog operationLog = new OperationLog();
        operationLog.setModule(joinPoint.getTarget().getClass().getSimpleName());
        operationLog.setOperation(joinPoint.getSignature().getName());
        operationLog.setMethod(joinPoint.getSignature().toShortString());
        operationLog.setCreateTime(LocalDateTime.now());

        try {
            Object result = joinPoint.proceed();
            operationLog.setStatus(1);
            return result;
        } catch (Throwable exception) {
            operationLog.setStatus(0);
            throw exception;
        } finally {
            operationLog.setCostTime(System.currentTimeMillis() - start);
            // 审计记录不能影响主业务结果，日志写入失败只记录并继续返回原结果。
            try {
                mapper.insert(operationLog);
            } catch (Exception ignored) {
                // 避免日志表故障导致管理操作失败。
            }
        }
    }
}
