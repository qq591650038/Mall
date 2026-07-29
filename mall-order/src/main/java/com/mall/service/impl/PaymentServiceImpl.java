package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.result.ErrorCode;
import com.mall.dto.order.PaymentCallbackDTO;
import com.mall.entity.Order;
import com.mall.entity.OrderItem;
import com.mall.entity.Payment;
import com.mall.exception.BusinessException;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.PaymentMapper;
import com.mall.mapper.ProductMapper;
import com.mall.service.PaymentService;
import com.mall.service.NotificationService;
import com.mall.service.MarketingActivityService;
import com.mall.service.PointsService;
import com.mall.vo.GroupPaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.scheduling.annotation.Scheduled;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.util.List;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;
    private final String secret;
    private final MarketingActivityService marketingActivityService;
    private final PointsService pointsService;
    private final boolean mockEnabled;

    public PaymentServiceImpl(PaymentMapper paymentMapper, OrderMapper orderMapper, OrderItemMapper orderItemMapper,
            ProductMapper productMapper,
            NotificationService notificationService,
            MarketingActivityService marketingActivityService,
            PointsService pointsService,
            @Value("${payment.callback-secret}") String secret,
            @Value("${payment.mock-enabled:false}") boolean mockEnabled) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.notificationService = notificationService;
        this.marketingActivityService = marketingActivityService;
        this.pointsService = pointsService;
        this.secret = secret;
        this.mockEnabled = mockEnabled;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void callback(PaymentCallbackDTO callback) {
        String payload = callback.getPaymentNo() + "|" + callback.getOrderNo() + "|" + callback.getAmount() + "|"
                + callback.getTimestamp();
        // 回调进入数据库前必须完成验签，防止伪造通知修改支付状态。
        if (!validSignature(payload, callback.getSignature())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "支付签名无效");
        }
        try {
            if (Math.abs(Instant.now().getEpochSecond() - Long.parseLong(callback.getTimestamp())) > 300) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "支付回调已过期");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付时间戳无效");
        }
        Payment payment = paymentMapper.selectOne(new QueryWrapper<Payment>().eq("payment_no", callback.getPaymentNo())
                .eq("order_no", callback.getOrderNo()));
        if (payment == null)
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付记录不存在");
        BigDecimal callbackAmount;
        try {
            callbackAmount = new BigDecimal(callback.getAmount()).setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付金额格式无效");
        }
        if (payment.getAmount() == null || payment.getAmount().compareTo(callbackAmount) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付金额不匹配");
        }
        processPaidPayment(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmMockPayment(Long userId, Long orderId, String paymentNo) {
        if (!mockEnabled) throw new BusinessException(ErrorCode.FORBIDDEN, "模拟支付未启用");
        Payment payment = paymentMapper.selectOne(new QueryWrapper<Payment>()
                .eq("payment_no", paymentNo).eq("order_id", orderId).last("LIMIT 1"));
        Order order = orderMapper.selectById(orderId);
        if (payment == null || order == null || !java.util.Objects.equals(order.getUserId(), userId)
                || !java.util.Objects.equals(payment.getOrderNo(), order.getOrderNo())
                || payment.getAmount() == null || payment.getAmount().compareTo(order.getPayAmount()) != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付记录不存在或不属于当前用户");
        }
        processPaidPayment(payment);
    }

    private void processPaidPayment(Payment payment) {
        if (payment.getPaymentStatus() == 1)
            return;
        if (paymentMapper.markPaid(payment.getPaymentNo()) != 1)
            return;
        if (orderMapper.markPaid(payment.getOrderId()) != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "order payment state update failed");
        }
        if (marketingActivityService != null) {
            GroupPaymentResult groupResult = marketingActivityService.onPaymentSuccessByOrderId(payment.getOrderId());
            if (groupResult.groupBuy() && !groupResult.groupFormed()) {
                Order waiting = orderMapper.selectById(payment.getOrderId());
                if (waiting != null) {
                    waiting.setOrderStatus(com.mall.common.result.OrderStatus.GROUP_PENDING.getCode());
                    waiting.setUpdateTime(java.time.LocalDateTime.now());
                    orderMapper.updateById(waiting);
                }
            } else {
                for (Long readyOrderId : groupResult.readyOrderIds()) {
                    Order ready = orderMapper.selectById(readyOrderId);
                    if (ready != null && com.mall.common.result.OrderStatus.GROUP_PENDING.getCode()
                            .equals(ready.getOrderStatus())) {
                        ready.setOrderStatus(com.mall.common.result.OrderStatus.PAID.getCode());
                        ready.setUpdateTime(java.time.LocalDateTime.now());
                        orderMapper.updateById(ready);
                    }
                }
            }
        }
        List<OrderItem> items = orderItemMapper
                .selectList(new QueryWrapper<OrderItem>().eq("order_id", payment.getOrderId()));
        for (OrderItem item : items) {
            com.mall.entity.Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setSales(product.getSales() + item.getQuantity());
                productMapper.updateById(product);
            }
        }

        // 支付成功后发送站内消息通知
        Order order = orderMapper.selectById(payment.getOrderId());
        if (order != null) {
            notificationService.notify(
                    order.getUserId(),
                    "PAYMENT_SUCCESS",
                    "支付成功",
                    "您的订单 " + order.getOrderNo() + " 支付成功，感谢您的购买！",
                    "ORDER",
                    order.getId());

            if (pointsService != null) {
                Long userId = order.getUserId();
                Long orderId = order.getId();
                String orderNo = order.getOrderNo();
                BigDecimal paidAmount = payment.getAmount();
                Runnable awardPoints = () -> {
                    try {
                        pointsService.earnForPayment(userId, paidAmount, orderId, orderNo);
                    } catch (Exception e) {
                        log.error("支付成功但积分发放失败，等待后续补偿: orderId={}", orderId, e);
                    }
                };
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            awardPoints.run();
                        }
                    });
                } else {
                    awardPoints.run();
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${mall.points.payment-retry-scan-ms:180000}")
    public void retryPaymentPoints() {
        if (pointsService == null) return;
        List<Payment> paid = paymentMapper.findPaidWithoutPoints(100);
        for (Payment payment : paid) {
            Order order = orderMapper.selectById(payment.getOrderId());
            if (order == null) continue;
            try {
                pointsService.earnForPayment(order.getUserId(), payment.getAmount(), order.getId(), order.getOrderNo());
            } catch (Exception e) {
                log.error("补发支付积分失败: orderId={}", order.getId(), e);
            }
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean validSignature(String payload, String signature) {
        if (signature == null || !signature.matches("(?i)[0-9a-f]{64}")) return false;
        return MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.US_ASCII),
                signature.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
}
