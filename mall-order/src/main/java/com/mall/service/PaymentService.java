package com.mall.service;
import com.mall.dto.order.PaymentCallbackDTO;
public interface PaymentService {
    void callback(PaymentCallbackDTO callback);
    void confirmMockPayment(Long userId, Long orderId, String paymentNo);
}
