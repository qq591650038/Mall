package com.mall.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.result.ErrorCode;
import com.mall.common.result.OrderStatus;
import com.mall.common.result.OrderStateMachine;
import com.mall.dto.order.CreateOrderDTO;
import com.mall.dto.order.CreateOrderDTO.OrderItemDTO;
import com.mall.entity.*;
import com.mall.exception.BusinessException;
import com.mall.mapper.*;
import com.mall.service.CouponService;
import com.mall.service.MarketingActivityService;
import com.mall.service.OrderService;
import com.mall.service.InventoryService;
import com.mall.service.NotificationService;
import com.mall.utils.RedisUtil;
import com.mall.vo.OrderVO;
import com.mall.vo.OrderVO.AddressSnapshot;
import com.mall.vo.OrderVO.OrderItemVO;
import com.mall.vo.OrderVO.OrderTimelineVO;
import com.mall.vo.PayResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Map;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final AddressMapper addressMapper;
    private final CartMapper cartMapper;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final PaymentMapper paymentMapper;
    private final CouponService couponService;
    private final UserCouponMapper userCouponMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final MarketingActivityService marketingActivityService;

    @Value("${mall.order.pending-timeout-minutes:30}")
    private long pendingTimeoutMinutes;

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            ProductMapper productMapper,
                            ProductSkuMapper productSkuMapper,
                            AddressMapper addressMapper,
                            CartMapper cartMapper,
                            RedisUtil redisUtil,
                            ObjectMapper objectMapper,
                            InventoryService inventoryService,
                            PaymentMapper paymentMapper,
                            CouponService couponService,
                            UserCouponMapper userCouponMapper,
                            UserMapper userMapper,
                            NotificationService notificationService,
                            MarketingActivityService marketingActivityService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.addressMapper = addressMapper;
        this.cartMapper = cartMapper;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
        this.paymentMapper = paymentMapper;
        this.couponService = couponService;
        this.userCouponMapper = userCouponMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.marketingActivityService = marketingActivityService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, CreateOrderDTO dto) {
        return createOrderInternal(userId, dto, Map.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createMarketingOrder(Long userId, CreateOrderDTO dto, Map<Long, BigDecimal> activityPrices) {
        return createOrderInternal(userId, dto, activityPrices == null ? Map.of() : activityPrices);
    }

    private OrderVO createOrderInternal(Long userId, CreateOrderDTO dto, Map<Long, BigDecimal> activityPrices) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已被删除，请重新登录");
        }

        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货地址不存在");
        }

        List<Long> skuIds = dto.getItems().stream().map(OrderItemDTO::getSkuId).distinct().collect(Collectors.toList());
        List<Long> productIds = dto.getItems().stream().map(OrderItemDTO::getProductId).distinct().collect(Collectors.toList());

        Map<Long, ProductSku> skuMap = productSkuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, Integer> quantityBySku = new LinkedHashMap<>();
        for (OrderItemDTO item : dto.getItems()) {
            ProductSku sku = skuMap.get(item.getSkuId());
            Product product = productMap.get(item.getProductId());
            if (sku == null || product == null || !Objects.equals(sku.getProductId(), item.getProductId())) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST, "商品与SKU不匹配");
            }
            if (product.getStatus() != 1 || !Objects.equals(sku.getStatus(), 1)) {
                throw new BusinessException(ErrorCode.PRODUCT_OFF_SHELF, "商品已下架");
            }
            int total = quantityBySku.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
            if (total <= 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "购买数量超出范围");
            }
            if (sku.getStock() < total) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足");
            }
        }
        for (Map.Entry<Long, Integer> entry : quantityBySku.entrySet()) {
            Long productId = dto.getItems().stream().filter(i -> i.getSkuId().equals(entry.getKey())).findFirst().get().getProductId();
            inventoryService.reserve(entry.getKey(), productId, entry.getValue());
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setAddressId(dto.getAddressId());
        order.setRemark(dto.getRemark());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setPayStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setExpireTime(LocalDateTime.now().plusMinutes(pendingTimeoutMinutes));
        order.setDeleted(0);

        AddressSnapshot snapshot = new AddressSnapshot();
        snapshot.setReceiverName(address.getReceiverName());
        snapshot.setReceiverPhone(address.getReceiverPhone());
        snapshot.setProvince(address.getProvince());
        snapshot.setCity(address.getCity());
        snapshot.setDistrict(address.getDistrict());
        snapshot.setDetailAddress(address.getDetailAddress());
        order.setAddressSnapshot(toJson(snapshot));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDTO item : dto.getItems()) {
            ProductSku sku = skuMap.get(item.getSkuId());
            Product product = productMap.get(item.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(item.getProductId());
            orderItem.setSkuId(item.getSkuId());
            orderItem.setProductName(product.getName());
            orderItem.setSkuInfo(sku.getSpecInfo());
            orderItem.setProductImage(sku.getImage() != null ? sku.getImage() : product.getMainImage());
            BigDecimal unitPrice = activityPrices.getOrDefault(item.getSkuId(), sku.getPrice());
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "订单价格无效");
            }
            orderItem.setPrice(unitPrice);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItem.setCreateTime(LocalDateTime.now());
            orderItems.add(orderItem);

            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFreightAmount(BigDecimal.ZERO);

        if (dto.getCouponId() != null) {
            UserCoupon userCoupon = userCouponMapper.selectById(dto.getCouponId());
            if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
            }
            if (userCoupon.getStatus() != 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "优惠券已被使用或已过期");
            }

            Coupon coupon = couponService.getById(userCoupon.getCouponId());
            if (coupon.getStatus() != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "优惠券已禁用");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
                throw new BusinessException(ErrorCode.CONFLICT, "优惠券不在有效期内");
            }

            if (totalAmount.compareTo(coupon.getMinAmount()) < 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "未达到优惠券使用门槛");
            }

            BigDecimal discount = calculateCouponDiscount(coupon, totalAmount);
            order.setCouponId(userCoupon.getId());
            order.setDiscountAmount(discount);
            order.setPayAmount(totalAmount.subtract(discount));

            log.info("锁定优惠券: userCouponId={}", userCoupon.getId());
        }

        orderMapper.insert(order);

        if (dto.getCouponId() != null) {
            // 订单获得数据库主键后再锁券，保证优惠券能够准确关联订单，便于取消和补偿审计。
            couponService.lockCoupon(dto.getCouponId(), order.getId());
        }

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        List<Long> cartSkuIds = dto.getItems().stream().map(OrderItemDTO::getSkuId).collect(Collectors.toList());
        List<Cart> carts = cartMapper.selectList(
                new QueryWrapper<Cart>().eq("user_id", userId).in("sku_id", cartSkuIds)
        );
        if (!carts.isEmpty()) {
            cartMapper.deleteBatchIds(carts.stream().map(Cart::getId).collect(Collectors.toList()));
        }

        String orderKey = "order:" + order.getOrderNo();
        redisUtil.set(orderKey, order.getId(), 30, TimeUnit.MINUTES);

        log.info("创建订单成功: orderNo={}, userId={}", order.getOrderNo(), userId);

        return convertToVO(order);
    }

    @Override
    public OrderVO getById(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        return convertToVO(order);
    }

    @Override
    public Page<OrderVO> pageByUserId(Long userId, Integer current, Integer size, Integer orderStatus) {
        Page<Order> page = new Page<>(current, size);
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (orderStatus != null) {
            wrapper.eq("order_status", orderStatus);
        }
        wrapper.orderByDesc("create_time");

        Page<Order> orderPage = orderMapper.selectPage(page, wrapper);
        List<Order> orders = orderPage.getRecords();

        if (orders.isEmpty()) {
            Page<OrderVO> result = new Page<>(current, size);
            result.setTotal(orderPage.getTotal());
            result.setRecords(List.of());
            return result;
        }

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().in("order_id", orderIds)
        );
        Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderVO> voList = orders.stream()
                .map(order -> convertToVO(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
                .collect(Collectors.toList());

        Page<OrderVO> result = new Page<>(current, size);
        result.setTotal(orderPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        OrderStatus status = OrderStatus.fromCode(order.getOrderStatus());
        if (!OrderStateMachine.canCancel(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        if (status == OrderStatus.PAID && order.getPayStatus() == 1) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL, "已支付订单无法取消");
        }

        if (orderMapper.updateStatus(id, userId, OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.CANCELLED.getCode()) != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态已变更");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", id)
        );
        for (OrderItem item : items) {
            inventoryService.release(item.getSkuId(), item.getProductId(), item.getQuantity());
        }

        onOrderCancel(id);

        if (order.getCouponId() != null) {
            couponService.releaseCoupon(order.getCouponId());
            log.info("释放优惠券: userCouponId={}", order.getCouponId());
        }

        log.info("取消订单: orderNo={}", order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaymentSuccess(Long orderId) {
        // Activity linkage is finalized by the marketing module through this hook.
        if (marketingActivityService != null) marketingActivityService.onPaymentSuccessByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCancel(Long orderId) {
        if (marketingActivityService != null) marketingActivityService.onOrderCancelByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResultVO payOrder(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        if (!OrderStateMachine.canPay(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setPaymentNo("PAY" + IdUtil.fastSimpleUUID().toUpperCase());
        payment.setAmount(order.getPayAmount());
        payment.setPaymentMethod(1);
        payment.setPaymentStatus(0);
        paymentMapper.insert(payment);
        log.info("创建支付流水: orderNo={}, paymentNo={}", order.getOrderNo(), payment.getPaymentNo());

        PayResultVO vo = new PayResultVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setAmount(payment.getAmount());
        vo.setPaymentMethod(1);
        vo.setPaymentStatus(0);
        vo.setExpireSeconds(1800);
        vo.setPayUrl("/mock-pay?orderNo=" + order.getOrderNo() + "&paymentNo=" + payment.getPaymentNo());
        return vo;
    }

    @Scheduled(fixedDelayString = "${mall.order.cancel-scan-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expired = orderMapper.selectList(new QueryWrapper<Order>()
                .eq("order_status", OrderStatus.PENDING_PAYMENT.getCode())
                .isNotNull("expire_time")
                .lt("expire_time", now)
                .last("LIMIT 100"));
        for (Order order : expired) {
            if (orderMapper.cancelPending(order.getId()) == 1) {
                List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", order.getId()));
                for (OrderItem item : items) {
                    inventoryService.release(item.getSkuId(), item.getProductId(), item.getQuantity());
                }
                onOrderCancel(order.getId());
                log.info("超时取消订单: orderNo={}", order.getOrderNo());
            }
        }
    }

    @Scheduled(cron = "${mall.order.auto-receive-cron:0 0 * * * *}")
    @Transactional(rollbackFor = Exception.class)
    public void autoConfirmShippedOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        List<Order> shipped = orderMapper.selectList(new QueryWrapper<Order>()
                .eq("order_status", OrderStatus.SHIPPED.getCode()).lt("ship_time", deadline).last("LIMIT 100"));
        for (Order order : shipped) {
            order.setOrderStatus(OrderStatus.COMPLETED.getCode()); order.setReceiveTime(LocalDateTime.now()); order.setUpdateTime(LocalDateTime.now()); orderMapper.updateById(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        if (!OrderStatus.SHIPPED.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CONFIRM);
        }

        order.setOrderStatus(OrderStatus.COMPLETED.getCode());
        order.setReceiveTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 确认收货后发送站内消息通知
        notificationService.notify(
                userId,
                "ORDER_COMPLETED",
                "交易完成",
                "您的订单 " + order.getOrderNo() + " 已确认收货，交易完成。欢迎再次光临！",
                "ORDER",
                order.getId()
        );

        log.info("确认收货: orderNo={}", order.getOrderNo());
    }

    private BigDecimal calculateCouponDiscount(Coupon coupon, BigDecimal totalAmount) {
        return switch (coupon.getType()) {
            case 1, 3 -> coupon.getValue();
            case 2 -> totalAmount.multiply(BigDecimal.valueOf(100).subtract(coupon.getValue()))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            default -> BigDecimal.ZERO;
        };
    }

    private String generateOrderNo() {
        return "MALL" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    private OrderVO convertToVO(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", order.getId())
        );
        return convertToVO(order, items);
    }

    private OrderVO convertToVO(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPayStatus(order.getPayStatus());
        vo.setPayTime(order.getPayTime());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusText(OrderStatus.getTextByCode(order.getOrderStatus()));
        vo.setShipTime(order.getShipTime());
        vo.setReceiveTime(order.getReceiveTime());
        vo.setLogisticsCompany(order.getLogisticsCompany());
        vo.setLogisticsNo(order.getLogisticsNo());
        vo.setAutoConfirmDeadline(order.getAutoConfirmDeadline());
        vo.setExpireTime(order.getExpireTime());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setAddressSnapshot(fromJson(order.getAddressSnapshot()));

        if (order.getAutoConfirmDeadline() != null && OrderStatus.SHIPPED.getCode().equals(order.getOrderStatus())) {
            long hours = Duration.between(LocalDateTime.now(), order.getAutoConfirmDeadline()).toHours();
            vo.setTimeoutHours(hours < 0 ? -1 : (int) hours);
        } else {
            vo.setTimeoutHours(0);
        }

        vo.setItems(items.stream().map(this::convertToItemVO).collect(Collectors.toList()));
        vo.setTimeline(buildTimeline(order));

        return vo;
    }

    private List<OrderTimelineVO> buildTimeline(Order order) {
        List<OrderTimelineVO> timeline = new ArrayList<>();

        OrderTimelineVO created = new OrderTimelineVO();
        created.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        created.setStatusText(OrderStatus.getTextByCode(OrderStatus.PENDING_PAYMENT.getCode()));
        created.setTime(order.getCreateTime());
        created.setDescription("订单创建");
        timeline.add(created);

        if (order.getPayTime() != null) {
            OrderTimelineVO paid = new OrderTimelineVO();
            paid.setStatus(OrderStatus.PAID.getCode());
            paid.setStatusText(OrderStatus.getTextByCode(OrderStatus.PAID.getCode()));
            paid.setTime(order.getPayTime());
            paid.setDescription("支付成功");
            timeline.add(paid);
        }

        if (order.getShipTime() != null) {
            OrderTimelineVO shipped = new OrderTimelineVO();
            shipped.setStatus(OrderStatus.SHIPPED.getCode());
            shipped.setStatusText(OrderStatus.getTextByCode(OrderStatus.SHIPPED.getCode()));
            shipped.setTime(order.getShipTime());
            String shipDesc = "商品已发货";
            if (order.getLogisticsCompany() != null && order.getLogisticsNo() != null) {
                shipDesc = order.getLogisticsCompany() + " " + order.getLogisticsNo();
            }
            shipped.setDescription(shipDesc);
            timeline.add(shipped);
        }

        if (order.getReceiveTime() != null) {
            OrderTimelineVO completed = new OrderTimelineVO();
            completed.setStatus(OrderStatus.COMPLETED.getCode());
            completed.setStatusText(OrderStatus.getTextByCode(OrderStatus.COMPLETED.getCode()));
            completed.setTime(order.getReceiveTime());
            completed.setDescription("确认收货，交易完成");
            timeline.add(completed);
        }

        if (OrderStatus.CANCELLED.getCode().equals(order.getOrderStatus())) {
            OrderTimelineVO cancelled = new OrderTimelineVO();
            cancelled.setStatus(OrderStatus.CANCELLED.getCode());
            cancelled.setStatusText(OrderStatus.getTextByCode(OrderStatus.CANCELLED.getCode()));
            cancelled.setTime(order.getUpdateTime());
            cancelled.setDescription("订单已取消");
            timeline.add(cancelled);
        }

        return timeline;
    }

    private String toJson(AddressSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("序列化地址快照失败", e);
            throw new BusinessException("地址序列化失败", e);
        }
    }

    private AddressSnapshot fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AddressSnapshot.class);
        } catch (Exception e) {
            log.warn("解析地址快照失败: {}", e.getMessage());
            return null;
        }
    }

    private OrderItemVO convertToItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setSkuId(item.getSkuId());
        vo.setProductName(item.getProductName());
        vo.setSkuInfo(item.getSkuInfo());
        vo.setProductImage(item.getProductImage());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setSubtotal(item.getSubtotal());
        return vo;
    }
}
