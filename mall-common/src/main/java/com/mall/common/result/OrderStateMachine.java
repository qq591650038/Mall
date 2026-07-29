package com.mall.common.result;

public final class OrderStateMachine {
    private OrderStateMachine() {
    }

    public static boolean canCancel(Integer status) {
        return status != null && (status == 0 || status == 1);
    }

    public static boolean canPay(Integer status) {
        return status != null && status == 0;
    }

    public static boolean canShip(Integer status) {
        return status != null && status == 1;
    }

    public static boolean canConfirm(Integer status) {
        return status != null && status == 2;
    }
}
