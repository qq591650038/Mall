package com.mall.exception;
import com.mall.common.result.ErrorCode;
import lombok.Getter;
@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;
    public BusinessException(String message){super(message);code=500;}
    public BusinessException(String message, Throwable cause){super(message,cause);code=500;}
    public BusinessException(Integer code,String message){super(message);this.code=code;}
    public BusinessException(ErrorCode error){super(error.getMessage());code=error.getCode();}
    public BusinessException(ErrorCode error,String message){super(message);code=error.getCode();}
    public BusinessException(ErrorCode error,Throwable cause){super(error.getMessage(),cause);code=error.getCode();}
}
