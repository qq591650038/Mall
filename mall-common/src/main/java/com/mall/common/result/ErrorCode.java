package com.mall.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_ALREADY_EXIST(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    PASSWORD_NOT_MATCH(1004, "两次密码不一致"),
    VERIFY_CODE_ERROR(1005, "验证码错误"),
    VERIFY_CODE_EXPIRED(1006, "验证码已过期"),
    PHONE_ALREADY_REGISTERED(1007, "手机号已注册"),
    EMAIL_ALREADY_REGISTERED(1008, "邮箱已注册"),
    PRODUCT_NOT_EXIST(2001, "商品不存在"),
    PRODUCT_OFF_SHELF(2002, "商品已下架"),
    SKU_NOT_EXIST(2003, "商品规格不存在"),
    STOCK_INSUFFICIENT(2004, "库存不足"),
    ORDER_NOT_EXIST(3001, "订单不存在"),
    ORDER_CANNOT_CANCEL(3002, "订单无法取消"),
    ORDER_CANNOT_CONFIRM(3003, "订单无法确认收货"),
    ORDER_STATUS_ERROR(3004, "订单状态错误"),
    REFUND_NOT_EXIST(3005, "退款记录不存在"),
    REFUND_STATUS_ERROR(3006, "退款状态错误"),
    CART_NOT_EXIST(4001, "购物车记录不存在"),
    TOKEN_INVALID(5001, "Token无效"),
    TOKEN_EXPIRED(5002, "Token已过期"),
    PERMISSION_DENIED(5003, "权限不足"),
    POINTS_INSUFFICIENT(6001, "积分余额不足"),
    POINTS_PRODUCT_NOT_EXIST(6002, "兑换商品不存在"),
    POINTS_PRODUCT_OFF_SHELF(6003, "兑换商品已下架"),
    POINTS_STOCK_INSUFFICIENT(6004, "兑换商品库存不足"),
    POINTS_ALREADY_CHECKIN(6005, "今日已签到"),
    COUPON_NOT_EXIST(7001, "优惠券不存在"),
    COUPON_ALREADY_USED(7002, "优惠券已被使用"),
    COUPON_EXPIRED(7003, "优惠券已过期"),
    COUPON_LOCKED(7004, "优惠券已被锁定"),
    COUPON_NOT_LOCKED(7005, "优惠券未被锁定"),
    DUPLICATE(8001, "数据重复"),
    ACTIVITY_NOT_EXIST(8002, "活动不存在"),
    ACTIVITY_NOT_STARTED(8003, "活动尚未开始"),
    ACTIVITY_ENDED(8004, "活动已结束"),
    ACTIVITY_CANCELLED(8005, "活动已取消"),
    ACTIVITY_STOCK_INSUFFICIENT(8006, "活动库存不足"),
    ACTIVITY_LIMIT_EXCEEDED(8007, "超过限购数量");
    private final Integer code;
    private final String message;

    ErrorCode(Integer c, String m) {
        code = c;
        message = m;
    }
}
