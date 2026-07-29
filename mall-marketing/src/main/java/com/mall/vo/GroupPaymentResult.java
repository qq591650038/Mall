package com.mall.vo;

import java.util.List;

public record GroupPaymentResult(boolean marketingOrder,
                                 boolean groupBuy,
                                 boolean groupFormed,
                                 List<Long> readyOrderIds) {
    public static GroupPaymentResult normalOrder() {
        return new GroupPaymentResult(false, false, true, List.of());
    }

    public static GroupPaymentResult marketingOrder(Long orderId) {
        return new GroupPaymentResult(true, false, true, List.of(orderId));
    }

    public static GroupPaymentResult groupPending() {
        return new GroupPaymentResult(true, true, false, List.of());
    }

    public static GroupPaymentResult groupFormed(List<Long> orderIds) {
        return new GroupPaymentResult(true, true, true, List.copyOf(orderIds));
    }
}
