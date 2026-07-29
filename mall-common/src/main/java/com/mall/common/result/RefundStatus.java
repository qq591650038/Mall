package com.mall.common.result;

import lombok.Getter;

@Getter
public enum RefundStatus {
    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REFUNDING(2, "退款中"),
    REFUNDED(3, "已退款"),
    REJECTED(4, "拒绝"),
    RETURN_SHIPPING(5, "退货中"),
    EXCHANGE_SHIPPING(6, "换货中"),
    FAILED(7, "退款失败");
    private final Integer code;
    private final String text;

    RefundStatus(Integer c, String t) {
        code = c;
        text = t;
    }

    public static String getTextByCode(Integer c) {
        for (RefundStatus s : values()) if (s.code.equals(c)) return s.text;
        return "未知";
    }
}
