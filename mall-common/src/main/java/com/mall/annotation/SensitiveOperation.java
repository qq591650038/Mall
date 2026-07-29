package com.mall.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感操作注解。
 * 标注在需要二次身份验证的管理员操作方法上。
 * 被标注的方法在执行前会校验请求头中的 X-Verify-Token，
 * 该Token需通过管理员密码验证接口获取，有效期5分钟。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveOperation {

    /**
     * 操作描述，用于日志记录和提示信息
     */
    String value() default "";
}