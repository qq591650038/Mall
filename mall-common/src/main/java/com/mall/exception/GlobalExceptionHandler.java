package com.mall.exception;

import com.mall.common.result.ErrorCode;
import com.mall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Void> business(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> validation(MethodArgumentNotValidException e) {
        return Result.error(ErrorCode.BAD_REQUEST, e.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", ")));
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> bind(BindException e) {
        return Result.error(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> mismatch(MethodArgumentTypeMismatchException e) {
        return Result.error(ErrorCode.BAD_REQUEST, "请求参数类型错误");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> unreadable(HttpMessageNotReadableException e) {
        return Result.error(ErrorCode.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> auth(AuthenticationException e) {
        return Result.error(ErrorCode.UNAUTHORIZED, "认证失败");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> other(Exception e) {
        log.error("系统异常", e);
        return Result.error(ErrorCode.INTERNAL_ERROR, "系统异常，请稍后重试");
    }
}
