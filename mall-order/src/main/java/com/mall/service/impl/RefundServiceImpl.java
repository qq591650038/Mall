package com.mall.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.common.result.OrderStatus;
import com.mall.common.result.RefundStatus;
import com.mall.dto.order.RefundCallbackDTO;
import com.mall.entity.Order;
import com.mall.entity.Payment;
import com.mall.entity.Refund;
import com.mall.entity.User;
import com.mall.exception.BusinessException;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.PaymentMapper;
import com.mall.mapper.RefundMapper;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.UserMapper;
import com.mall.service.RefundService;
import com.mall.service.CouponService;
import com.mall.service.NotificationService;
import com.mall.service.InventoryService;
import com.mall.service.MarketingActivityService;
import com.mall.service.PointsService;
import com.mall.vo.OrderVO;
import com.mall.vo.RefundVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.time.Instant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Slf4j
@Service
public class RefundServiceImpl implements RefundService {

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final UserMapper userMapper;
    private final CouponService couponService;
    private final NotificationService notificationService;
    private final OrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;
    private final MarketingActivityService marketingActivityService;
    private final PointsService pointsService;
    private final String callbackSecret;

    public RefundServiceImpl(RefundMapper refundMapper,
                             OrderMapper orderMapper,
                             PaymentMapper paymentMapper,
                             UserMapper userMapper,
                             CouponService couponService,
                             NotificationService notificationService,
                             OrderItemMapper orderItemMapper,
                             InventoryService inventoryService,
                             MarketingActivityService marketingActivityService,
                             PointsService pointsService,
                             @Value("${payment.callback-secret}") String callbackSecret) {
        this.refundMapper = refundMapper;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.userMapper = userMapper;
        this.couponService = couponService;
        this.notificationService = notificationService;
        this.orderItemMapper = orderItemMapper;
        this.inventoryService = inventoryService;
        this.marketingActivityService = marketingActivityService;
        this.pointsService = pointsService;
        this.callbackSecret = callbackSecret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyGroupFailureRefund(Long orderId) {
        Order order = orderMapper.selectOne(new QueryWrapper<Order>().eq("id", orderId).last("FOR UPDATE"));
        if (order == null || !OrderStatus.GROUP_PENDING.getCode().equals(order.getOrderStatus())) return;
        Long existing = refundMapper.selectCount(new QueryWrapper<Refund>().eq("order_id", orderId)
                .in("status", RefundStatus.PENDING.getCode(), RefundStatus.APPROVED.getCode(),
                        RefundStatus.REFUNDING.getCode(), RefundStatus.REFUNDED.getCode()));
        if (existing > 0) return;
        Payment payment = paymentMapper.selectOne(new QueryWrapper<Payment>()
                .eq("order_id", orderId).eq("payment_status", 1).last("LIMIT 1"));
        if (payment == null) return;

        Refund refund = new Refund();
        refund.setOrderId(orderId);
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(order.getUserId());
        refund.setRefundNo("REF" + IdUtil.fastSimpleUUID().toUpperCase());
        refund.setAmount(order.getPayAmount());
        refund.setReason("拼团活动结束，未达到成团人数");
        refund.setType(0);
        refund.setStatus(RefundStatus.REFUNDING.getCode());
        refund.setPaymentNo(payment.getPaymentNo());
        refund.setRetryCount(0);
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        refund.setDeleted(0);
        refundMapper.insert(refund);

        order.setOrderStatus(OrderStatus.REFUNDING.getCode());
        order.setPayStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        notificationService.notify(order.getUserId(), "GROUP_BUY_FAILED", "拼团失败",
                "您的订单 " + order.getOrderNo() + " 未能成团，已自动发起退款。", "REFUND", refund.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO apply(Long userId, Long orderId, Refund refund) {
        Order order = orderMapper.selectOne(new QueryWrapper<Order>().eq("id", orderId).last("FOR UPDATE"));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }

        OrderStatus orderStatus = OrderStatus.fromCode(order.getOrderStatus());
        if (orderStatus != OrderStatus.PAID
                && orderStatus != OrderStatus.SHIPPED
                && orderStatus != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不允许申请退款");
        }
        if (refund.getAmount() == null || refund.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || refund.getAmount().compareTo(order.getPayAmount()) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "退款金额无效");
        }

        List<Refund> existingRefunds = refundMapper.selectList(
                new QueryWrapper<Refund>()
                        .eq("order_id", orderId)
                        .in("status", List.of(
                                RefundStatus.PENDING.getCode(),
                                RefundStatus.APPROVED.getCode(),
                                RefundStatus.REFUNDING.getCode(),
                                RefundStatus.RETURN_SHIPPING.getCode(),
                                RefundStatus.EXCHANGE_SHIPPING.getCode(),
                                RefundStatus.REFUNDED.getCode()
                        ))
        );
        if (existingRefunds.stream().anyMatch(r -> !RefundStatus.REFUNDED.getCode().equals(r.getStatus()))) {
            throw new BusinessException(ErrorCode.CONFLICT, "该订单已存在退款申请");
        }

        refund.setOrderId(orderId);
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(userId);
        refund.setRefundNo("REF" + IdUtil.fastSimpleUUID().toUpperCase());
        BigDecimal refundedTotal = existingRefunds.stream().map(Refund::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (refundedTotal.add(refund.getAmount()).compareTo(order.getPayAmount()) > 0)
            throw new BusinessException(ErrorCode.CONFLICT, "累计退款金额不能超过订单实付金额");
        refund.setAmount(refund.getAmount().setScale(2, java.math.RoundingMode.HALF_UP));
        Integer refundType = refund.getType() == null ? 0 : refund.getType();
        if (refundType != 0 && refundType != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "售后类型无效");
        }
        refund.setType(refundType);
        refund.setOriginalOrderStatus(order.getOrderStatus());
        refund.setStatus(RefundStatus.PENDING.getCode());
        refund.setRetryCount(0);
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        refund.setDeleted(0);
        refundMapper.insert(refund);

        order.setOrderStatus(OrderStatus.REFUNDING.getCode());
        order.setPayStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 退款申请后发送站内消息通知
        notificationService.notify(
                userId,
                "REFUND_APPLIED",
                "退款申请已提交",
                "您的订单 " + order.getOrderNo() + " 退款申请已提交，退款金额 ¥" + refund.getAmount() + "，请等待审核。",
                "REFUND",
                refund.getId()
        );

        log.info("申请退款: refundNo={}, orderNo={}, userId={}", refund.getRefundNo(), order.getOrderNo(), userId);

        return convertToVO(refund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long userId) {
        Refund refund = refundMapper.selectOne(new QueryWrapper<Refund>().eq("id", id).last("FOR UPDATE"));
        if (refund == null || !refund.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.NOT_FOUND, "退款记录不存在");
        if (!RefundStatus.PENDING.getCode().equals(refund.getStatus()))
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前退款状态不可撤销");
        refundMapper.deleteById(id);
        Order order = orderMapper.selectById(refund.getOrderId());
        if (order != null && OrderStatus.REFUNDING.getCode().equals(order.getOrderStatus())) {
            order.setOrderStatus(restoreOrderStatus(refund));
            order.setPayStatus(1);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    @Override
    public RefundVO getById(Long id, Long userId) {
        Refund refund = refundMapper.selectOne(new QueryWrapper<Refund>().eq("id", id).last("FOR UPDATE"));
        if (refund == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "退款记录不存在");
        }
        if (userId != null && !refund.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "退款记录不存在");
        }
        return convertToVO(refund);
    }

    @Override
    public Page<RefundVO> pageByUserId(Long userId, Integer current, Integer size, Integer status) {
        Page<Refund> page = new Page<>(current, size);
        QueryWrapper<Refund> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");

        Page<Refund> refundPage = refundMapper.selectPage(page, wrapper);
        List<RefundVO> voList = refundPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();

        Page<RefundVO> result = new Page<>(current, size);
        result.setTotal(refundPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public Page<RefundVO> pageAdmin(Integer current, Integer size, Integer status, String orderNo) {
        Page<Refund> page = new Page<>(current, size);
        QueryWrapper<Refund> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.like("order_no", orderNo);
        }
        wrapper.orderByDesc("create_time");

        Page<Refund> refundPage = refundMapper.selectPage(page, wrapper);

        List<Refund> records = refundPage.getRecords();
        if (records.isEmpty()) {
            Page<RefundVO> result = new Page<>(current, size);
            result.setTotal(refundPage.getTotal());
            result.setRecords(List.of());
            return result;
        }

        List<Long> userIds = records.stream()
                .map(Refund::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> usernameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        List<RefundVO> voList = records.stream()
                .map(r -> {
                    RefundVO vo = convertToVO(r);
                    vo.setUsername(usernameMap.getOrDefault(r.getUserId(), "未知用户"));
                    return vo;
                })
                .toList();

        Page<RefundVO> result = new Page<>(current, size);
        result.setTotal(refundPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, Integer status, String remark) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "退款记录不存在");
        }
        if (!RefundStatus.PENDING.getCode().equals(refund.getStatus())) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR, "只有待审核申请可以审核");
        }
        if (!RefundStatus.APPROVED.getCode().equals(status) && !RefundStatus.REJECTED.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "审核状态只能是通过或拒绝");
        }

        refund.setStatus(status);
        refund.setReviewRemark(remark);
        refund.setReviewTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        if (RefundStatus.APPROVED.getCode().equals(status)) {
            Integer refundType = refund.getType() != null ? refund.getType() : 0;

            if (refundType == 0) {
                Payment payment = paymentMapper.selectOne(
                        new QueryWrapper<Payment>()
                                .eq("order_id", refund.getOrderId())
                                .eq("payment_status", 1)
                                .last("LIMIT 1")
                );
                if (payment != null) {
                    refund.setPaymentNo(payment.getPaymentNo());
                    refund.setStatus(RefundStatus.REFUNDING.getCode());
                    refund.setUpdateTime(LocalDateTime.now());
                    refundMapper.updateById(refund);
                }
                log.info("审核通过仅退款: refundNo={}", refund.getRefundNo());
            } else if (refundType == 1) {
                log.info("审核通过退货: refundNo={}", refund.getRefundNo());
            } else if (refundType == 2) {
                log.info("审核通过换货: refundNo={}", refund.getRefundNo());
            }

            notificationService.notify(
                    refund.getUserId(),
                    "REFUND_APPROVED",
                    "售后审核通过",
                    "您的订单 " + refund.getOrderNo() + " 售后申请已审核通过。",
                    "REFUND",
                    refund.getId()
            );
        } else if (RefundStatus.REJECTED.getCode().equals(status)) {
            Order order = orderMapper.selectById(refund.getOrderId());
            if (order != null) {
                order.setOrderStatus(restoreOrderStatus(refund));
                order.setPayStatus(1);
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }
            log.info("拒绝退款: refundNo={}", refund.getRefundNo());

            notificationService.notify(
                    refund.getUserId(),
                    "REFUND_REJECTED",
                    "售后申请被拒绝",
                    "您的订单 " + refund.getOrderNo() + " 售后申请未通过审核。",
                    "REFUND",
                    refund.getId()
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundSuccess(Long id) {
        Refund refund = refundMapper.selectOne(new QueryWrapper<Refund>().eq("id", id).last("FOR UPDATE"));
        if (refund == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "退款记录不存在");
        }
        if (RefundStatus.REFUNDED.getCode().equals(refund.getStatus())) return;
        if (!RefundStatus.REFUNDING.getCode().equals(refund.getStatus())) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR, "退款尚未进入第三方退款处理中");
        }
        refund.setStatus(RefundStatus.REFUNDED.getCode());
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        Order order = orderMapper.selectById(refund.getOrderId());
        if (order != null) {
            BigDecimal refundedAmount = refundMapper.selectList(
                            new QueryWrapper<Refund>().eq("order_id", refund.getOrderId())
                                    .eq("status", RefundStatus.REFUNDED.getCode()))
                    .stream().map(Refund::getAmount).filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean fullyRefunded = refundedAmount.compareTo(order.getPayAmount()) >= 0;
            if (fullyRefunded && !OrderStatus.REFUNDED.getCode().equals(order.getOrderStatus())) {
                orderItemMapper.selectList(new QueryWrapper<com.mall.entity.OrderItem>().eq("order_id", order.getId()))
                        .forEach(item -> inventoryService.release(item.getSkuId(), item.getProductId(), item.getQuantity()));
                marketingActivityService.onRefundSuccessByOrderId(order.getId());
                pointsService.reversePaymentPoints(order.getUserId(), order.getId(), order.getOrderNo());
            }
            order.setPayStatus(fullyRefunded ? 2 : 1);
            order.setOrderStatus(fullyRefunded ? OrderStatus.REFUNDED.getCode() : restoreOrderStatus(refund));
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            // A coupon belongs to the order and can be reused only after the
            // entire order has been refunded. releaseCoupon is conditional
            // on the locked state, so repeated callbacks remain idempotent.
            if (order.getCouponId() != null && fullyRefunded) {
                couponService.releaseCoupon(order.getCouponId());
                couponService.recordUsageEvent(order.getCouponId(), "REFUNDED", order.getId(), "订单退款完成，释放优惠券");
                log.info("退款完成，释放优惠券: userCouponId={}, orderNo={}", order.getCouponId(), order.getOrderNo());
            }
        }

        Order orderForNotify = orderMapper.selectById(refund.getOrderId());
        Payment payment = paymentMapper.selectOne(new QueryWrapper<Payment>()
                .eq("order_id", refund.getOrderId())
                .eq("payment_no", refund.getPaymentNo())
                .last("LIMIT 1"));
        if (payment != null && orderForNotify != null
                && OrderStatus.REFUNDED.getCode().equals(orderForNotify.getOrderStatus())) {
            payment.setPaymentStatus(2);
            payment.setUpdateTime(LocalDateTime.now());
            paymentMapper.updateById(payment);
        }

        // 退款成功后发送站内消息通知
        if (orderForNotify != null) {
            notificationService.notify(
                    orderForNotify.getUserId(),
                    "REFUND_SUCCESS",
                    "退款成功",
                    "您的订单 " + orderForNotify.getOrderNo() + " 退款已成功处理，退款金额 ¥" + refund.getAmount() + " 已返回原支付账户。",
                    "REFUND",
                    refund.getId()
            );
        }

        log.info("退款完成: refundNo={}", refund.getRefundNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void refundFailed(Long id, String reason) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null) throw new BusinessException(ErrorCode.NOT_FOUND, "退款记录不存在");
        if (!RefundStatus.REFUNDING.getCode().equals(refund.getStatus()))
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "退款状态错误");
        refund.setStatus(RefundStatus.FAILED.getCode());
        refund.setLastError(reason);
        refund.setReviewRemark(reason);
        refund.setRetryCount((refund.getRetryCount() == null ? 0 : refund.getRetryCount()) + 1);
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void callback(RefundCallbackDTO callback) {
        String payload = callback.getRefundNo() + "|" + callback.getOrderNo() + "|" + callback.getAmount() + "|" + callback.getTimestamp() + "|" + callback.getStatus();
        if (!validSignature(payload, callback.getSignature()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "退款签名无效");
        try {
            if (Math.abs(Instant.now().getEpochSecond() - Long.parseLong(callback.getTimestamp())) > 300)
                throw new BusinessException(ErrorCode.BAD_REQUEST, "退款回调已过期");
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "退款时间戳无效");
        }
        Refund refund = refundMapper.selectOne(new QueryWrapper<Refund>().eq("refund_no", callback.getRefundNo()).eq("order_no", callback.getOrderNo()).last("LIMIT 1"));
        if (refund == null) throw new BusinessException(ErrorCode.NOT_FOUND, "退款记录不存在");
        BigDecimal callbackAmount;
        try {
            callbackAmount = new BigDecimal(callback.getAmount()).setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "退款金额格式无效");
        }
        if (refund.getAmount() == null || refund.getAmount().compareTo(callbackAmount) != 0)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "退款金额不匹配");
        if (RefundStatus.REFUNDED.getCode().equals(refund.getStatus())) return;
        if ("SUCCESS".equalsIgnoreCase(callback.getStatus())) refundSuccess(refund.getId());
        else refundFailed(refund.getId(), "第三方退款失败: " + callback.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReturnLogistics(Long userId, Long refundId, String logisticsCompany, String logisticsNo) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null || !refund.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.REFUND_NOT_EXIST);

        if (!RefundStatus.APPROVED.getCode().equals(refund.getStatus())
                && !RefundStatus.RETURN_SHIPPING.getCode().equals(refund.getStatus()))
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR, "当前状态不允许提交退货物流");

        refund.setLogisticsCompany(logisticsCompany);
        refund.setLogisticsNo(logisticsNo);
        refund.setStatus(RefundStatus.RETURN_SHIPPING.getCode());
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        notificationService.notify(
                userId,
                "RETURN_LOGISTICS_SUBMITTED",
                "退货物流已提交",
                "您的退货物流信息已提交，物流公司：" + logisticsCompany + "，单号：" + logisticsNo + "，请耐心等待商家确认收货。",
                "REFUND",
                refund.getId()
        );

        log.info("用户提交退货物流: refundId={}, userId={}", refundId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReturnLogistics(Long refundId, String logisticsCompany, String logisticsNo) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null)
            throw new BusinessException(ErrorCode.REFUND_NOT_EXIST);

        refund.setLogisticsCompany(logisticsCompany);
        refund.setLogisticsNo(logisticsNo);
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        log.info("管理员更新退货物流: refundId={}", refundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExchangeLogistics(Long refundId, String trackingNo) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null)
            throw new BusinessException(ErrorCode.REFUND_NOT_EXIST);

        if (refund.getType() == null || refund.getType() != 2)
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR, "该退款不是换货类型");

        refund.setTrackingNo(trackingNo);
        refund.setStatus(RefundStatus.EXCHANGE_SHIPPING.getCode());
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        notificationService.notify(
                refund.getUserId(),
                "EXCHANGE_SHIPPED",
                "换货已发出",
                "您的换货新品已发出，物流单号：" + trackingNo + "，请注意查收。",
                "REFUND",
                refund.getId()
        );

        log.info("管理员更新换货发出物流: refundId={}", refundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturnReceived(Long userId, Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null || !refund.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.REFUND_NOT_EXIST);

        Integer status = refund.getStatus();
        if (!RefundStatus.RETURN_SHIPPING.getCode().equals(status)
                && !RefundStatus.EXCHANGE_SHIPPING.getCode().equals(status)
                && !RefundStatus.APPROVED.getCode().equals(status))
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR, "当前状态不允许确认收货");

        throw new BusinessException(ErrorCode.FORBIDDEN, "用户不能直接确认退款到账，请等待商家验收和支付渠道回调");
    }

    private boolean validSignature(String payload, String signature) {
        if (signature == null || !signature.matches("(?i)[0-9a-f]{64}")) return false;
        return MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.US_ASCII),
                signature.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    private int restoreOrderStatus(Refund refund) {
        return refund.getOriginalOrderStatus() == null
                ? OrderStatus.PAID.getCode()
                : refund.getOriginalOrderStatus();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO applyExchange(Long userId, Long orderId, Long exchangeProductId, Long exchangeSkuId, String reason) {
        Order order = orderMapper.selectOne(new QueryWrapper<Order>().eq("id", orderId).last("FOR UPDATE"));
        if (order == null || !order.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);

        OrderStatus orderStatus = OrderStatus.fromCode(order.getOrderStatus());
        if (orderStatus != OrderStatus.PAID
                && orderStatus != OrderStatus.SHIPPED
                && orderStatus != OrderStatus.COMPLETED)
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不允许申请换货");

        List<Refund> existingRefunds = refundMapper.selectList(
                new QueryWrapper<Refund>()
                        .eq("order_id", orderId)
                        .in("status", List.of(
                                RefundStatus.PENDING.getCode(),
                                RefundStatus.APPROVED.getCode(),
                                RefundStatus.REFUNDING.getCode(),
                                RefundStatus.RETURN_SHIPPING.getCode(),
                                RefundStatus.EXCHANGE_SHIPPING.getCode()
                        ))
        );
        if (!existingRefunds.isEmpty())
            throw new BusinessException(ErrorCode.CONFLICT, "该订单已存在售后申请");

        Refund refund = new Refund();
        refund.setOrderId(orderId);
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(userId);
        refund.setRefundNo("REF" + IdUtil.fastSimpleUUID().toUpperCase());
        refund.setAmount(order.getPayAmount());
        refund.setReason(reason);
        refund.setType(2);
        refund.setOriginalOrderStatus(order.getOrderStatus());
        refund.setExchangeProductId(exchangeProductId);
        refund.setExchangeSkuId(exchangeSkuId);
        refund.setStatus(RefundStatus.PENDING.getCode());
        refund.setRetryCount(0);
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        refund.setDeleted(0);
        refundMapper.insert(refund);

        order.setOrderStatus(OrderStatus.REFUNDING.getCode());
        order.setPayStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        notificationService.notify(
                userId,
                "EXCHANGE_APPLIED",
                "换货申请已提交",
                "您的订单 " + order.getOrderNo() + " 换货申请已提交，请等待审核。",
                "REFUND",
                refund.getId()
        );

        log.info("申请换货: refundNo={}, orderNo={}, userId={}", refund.getRefundNo(), order.getOrderNo(), userId);

        return convertToVO(refund);
    }

    @Scheduled(fixedDelayString = "${mall.refund.retry-scan-ms:180000}")
    @Transactional
    public void retryFailedRefunds() {
        List<Refund> failed = refundMapper.selectList(new QueryWrapper<Refund>().eq("status", RefundStatus.FAILED.getCode()).lt("retry_count", 3).last("LIMIT 50"));
        for (Refund refund : failed) {
            refund.setStatus(RefundStatus.REFUNDING.getCode());
            refund.setUpdateTime(LocalDateTime.now());
            refundMapper.updateById(refund);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private RefundVO convertToVO(Refund refund) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setOrderId(refund.getOrderId());
        vo.setOrderNo(refund.getOrderNo());
        vo.setRefundNo(refund.getRefundNo());
        vo.setAmount(refund.getAmount());
        vo.setReason(refund.getReason());
        vo.setImages(refund.getImages());
        vo.setStatus(refund.getStatus());
        vo.setStatusText(RefundStatus.getTextByCode(refund.getStatus()));
        vo.setReviewRemark(refund.getReviewRemark());
        vo.setReviewTime(refund.getReviewTime());
        vo.setPaymentNo(refund.getPaymentNo());
        vo.setCreateTime(refund.getCreateTime());
        vo.setType(refund.getType());
        vo.setLogisticsCompany(refund.getLogisticsCompany());
        vo.setLogisticsNo(refund.getLogisticsNo());
        vo.setReturnAddress(refund.getReturnAddress());
        vo.setTrackingNo(refund.getTrackingNo());
        vo.setExchangeProductId(refund.getExchangeProductId());
        vo.setExchangeSkuId(refund.getExchangeSkuId());
        return vo;
    }
}
