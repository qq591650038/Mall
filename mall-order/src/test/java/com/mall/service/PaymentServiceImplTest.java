package com.mall.service;

import com.mall.dto.order.PaymentCallbackDTO;
import com.mall.entity.Payment;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.PaymentMapper;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.ProductMapper;
import com.mall.service.impl.PaymentServiceImpl;
import com.mall.vo.GroupPaymentResult;
import com.mall.entity.Order;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

class PaymentServiceImplTest {
    private final PaymentMapper paymentMapper = mock(PaymentMapper.class);
    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl(paymentMapper, orderMapper,
            mock(OrderItemMapper.class), mock(ProductMapper.class), notificationService,
            null, null, "test-secret", true);

    @Test
    void invalidSignatureIsRejected() {
        PaymentCallbackDTO dto = callback("bad");
        assertThrows(RuntimeException.class, () -> service.callback(dto));
        verifyNoInteractions(paymentMapper, orderMapper);
    }

    @Test
    void repeatedCallbackIsIdempotent() throws Exception {
        Payment payment = new Payment();
        payment.setPaymentStatus(1);
        payment.setPaymentNo("p1");
        payment.setOrderNo("o1");
        payment.setAmount(new java.math.BigDecimal("10.00"));
        when(paymentMapper.selectOne(any(QueryWrapper.class))).thenReturn(payment);
        service.callback(callback(sign("p1|o1|10.00|" + timestamp())));
        verify(paymentMapper, never()).markPaid(any());
        verifyNoInteractions(orderMapper);
    }

    @Test
    void concurrentDuplicateCallbackStopsWhenPaymentCasLoses() throws Exception {
        Payment payment = new Payment();
        payment.setPaymentStatus(0);
        payment.setPaymentNo("p1");
        payment.setOrderNo("o1");
        payment.setOrderId(9L);
        payment.setAmount(new java.math.BigDecimal("10.00"));
        when(paymentMapper.selectOne(any(QueryWrapper.class))).thenReturn(payment);
        when(paymentMapper.markPaid("p1")).thenReturn(0);

        service.callback(callback(sign("p1|o1|10.00|" + timestamp())));

        verify(orderMapper, never()).markPaid(anyLong());
    }

    @Test
    void normalOrderIsNotChangedToGroupPending() throws Exception {
        MarketingActivityService marketingService = mock(MarketingActivityService.class);
        PaymentServiceImpl paymentService = serviceWithMarketing(marketingService);
        Payment payment = unpaidPayment();
        when(paymentMapper.selectOne(any(QueryWrapper.class))).thenReturn(payment);
        when(paymentMapper.markPaid("p1")).thenReturn(1);
        when(orderMapper.markPaid(9L)).thenReturn(1);
        when(marketingService.onPaymentSuccessByOrderId(9L)).thenReturn(GroupPaymentResult.normalOrder());

        paymentService.callback(callback(sign("p1|o1|10.00|" + timestamp())));

        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    void unformedGroupOrderBecomesGroupPending() throws Exception {
        MarketingActivityService marketingService = mock(MarketingActivityService.class);
        PaymentServiceImpl paymentService = serviceWithMarketing(marketingService);
        Payment payment = unpaidPayment();
        Order order = new Order();
        order.setId(9L);
        when(paymentMapper.selectOne(any(QueryWrapper.class))).thenReturn(payment);
        when(paymentMapper.markPaid("p1")).thenReturn(1);
        when(orderMapper.markPaid(9L)).thenReturn(1);
        when(orderMapper.selectById(9L)).thenReturn(order);
        when(marketingService.onPaymentSuccessByOrderId(9L)).thenReturn(GroupPaymentResult.groupPending());

        paymentService.callback(callback(sign("p1|o1|10.00|" + timestamp())));

        verify(orderMapper).updateById(argThat(updated -> updated.getOrderStatus() == 7));
    }

    private PaymentServiceImpl serviceWithMarketing(MarketingActivityService marketingService) {
        return new PaymentServiceImpl(paymentMapper, orderMapper, mock(OrderItemMapper.class),
                mock(ProductMapper.class), notificationService, marketingService,
                mock(PointsService.class), "test-secret", true);
    }

    private Payment unpaidPayment() {
        Payment payment = new Payment();
        payment.setPaymentStatus(0);
        payment.setPaymentNo("p1");
        payment.setOrderNo("o1");
        payment.setOrderId(9L);
        payment.setAmount(new java.math.BigDecimal("10.00"));
        return payment;
    }

    private PaymentCallbackDTO callback(String signature) {
        PaymentCallbackDTO d = new PaymentCallbackDTO();
        d.setPaymentNo("p1");
        d.setOrderNo("o1");
        d.setAmount("10.00");
        d.setTimestamp(timestamp());
        d.setSignature(signature);
        return d;
    }

    private String timestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
