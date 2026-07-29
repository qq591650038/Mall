package com.mall.common.result;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING_PAYMENT(0, "待付款"),
    PAID(1, "待发货"),
    SHIPPED(2, "待收货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款"),
    GROUP_PENDING(7, "待成团");

    private final Integer code;
    private final String text;

    OrderStatus(Integer c, String t) {
        code = c;
        text = t;
    }

    public static OrderStatus fromCode(Integer c) {
        if (c == null) return null;
        for (OrderStatus s : values()) if (s.code.equals(c)) return s;
        return null;
    }

    public static String getTextByCode(Integer c) {
        OrderStatus s = fromCode(c);
        return s == null ? "未知" : s.text;
    }
}
