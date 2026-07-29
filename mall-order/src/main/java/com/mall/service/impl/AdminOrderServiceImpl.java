package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.CursorPageResult;
import com.mall.common.result.ErrorCode;
import com.mall.common.result.OrderStatus;
import com.mall.common.util.CursorCodec;
import com.mall.entity.*;
import com.mall.exception.BusinessException;
import com.mall.mapper.*;
import com.mall.service.AdminOrderService;
import com.mall.service.NotificationService;
import com.mall.vo.OrderVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final NotificationService notificationService;

    public AdminOrderServiceImpl(OrderMapper o, OrderItemMapper i, UserMapper u, ProductMapper p, AddressMapper a,
                                 NotificationService notificationService) {
        orderMapper = o;
        itemMapper = i;
        userMapper = u;
        productMapper = p;
        addressMapper = a;
        this.notificationService = notificationService;
    }

    private Order getOrder(Long id) {
        Order o = orderMapper.selectById(id);
        if (o == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
        }
        return o;
    }

    @Override
    public CursorPageResult<OrderVO> cursorPage(Integer size, Integer status, String orderNo, Long userId, String cursor) {
        int limit = Math.min(Math.max(size == null ? 20 : size, 1), 100) + 1;
        CursorCodec.Decoded decoded = CursorCodec.decode(cursor);
        List<Order> rows = orderMapper.selectAdminCursorPage(status, orderNo, userId,
                decoded == null ? null : decoded.createTime(), decoded == null ? null : decoded.id(), limit);
        boolean hasNext = rows.size() == limit;
        List<Order> orders = hasNext ? rows.subList(0, limit - 1) : rows;
        return new CursorPageResult<>(toPageVOs(orders), hasNext ? cursorOf(orders.get(orders.size() - 1)) : null, hasNext);
    }

    private String cursorOf(Order order) {
        return CursorCodec.encode(order.getCreateTime(), order.getId());
    }

    private List<OrderVO> toPageVOs(List<Order> orders) {
        if (orders.isEmpty()) return List.of();
        List<Long> userIds = orders.stream().map(Order::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> usernameMap = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        return orders.stream().map(o -> {
            OrderVO vo = new OrderVO();
            vo.setId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setUsername(usernameMap.getOrDefault(o.getUserId(), "Unknown user"));
            vo.setTotalAmount(o.getTotalAmount());
            vo.setPayAmount(o.getPayAmount());
            vo.setOrderStatus(o.getOrderStatus());
            vo.setOrderStatusText(OrderStatus.getTextByCode(o.getOrderStatus()));
            vo.setCreateTime(o.getCreateTime());
            vo.setPayTime(o.getPayTime());
            vo.setShipTime(o.getShipTime());
            vo.setReceiveTime(o.getReceiveTime());
            vo.setLogisticsCompany(o.getLogisticsCompany());
            vo.setLogisticsNo(o.getLogisticsNo());
            vo.setExpireTime(o.getExpireTime());
            vo.setPayStatus(o.getPayStatus());
            return vo;
        }).toList();
    }

    @Override
    public Page<OrderVO> page(Integer c, Integer s, Integer st, String no, Long uid) {
        Page<Order> p = new Page<>(c, s);
        QueryWrapper<Order> w = new QueryWrapper<>();
        if (st != null) {
            w.eq("order_status", st);
        }
        if (no != null && !no.isBlank()) {
            w.like("order_no", no);
        }
        if (uid != null) {
            w.eq("user_id", uid);
        }
        w.orderByDesc("create_time");
        Page<Order> orderPage = orderMapper.selectPage(p, w);

        List<Order> orders = orderPage.getRecords();
        if (orders.isEmpty()) {
            Page<OrderVO> result = new Page<>(c, s);
            result.setTotal(orderPage.getTotal());
            result.setRecords(List.of());
            return result;
        }

        List<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> usernameMap = userIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        List<OrderVO> voList = orders.stream().map(o -> {
            OrderVO vo = new OrderVO();
            vo.setId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setUsername(usernameMap.getOrDefault(o.getUserId(), "未知用户"));
            vo.setTotalAmount(o.getTotalAmount());
            vo.setPayAmount(o.getPayAmount());
            vo.setOrderStatus(o.getOrderStatus());
            vo.setOrderStatusText(OrderStatus.getTextByCode(o.getOrderStatus()));
            vo.setCreateTime(o.getCreateTime());
            vo.setPayTime(o.getPayTime());
            vo.setShipTime(o.getShipTime());
            vo.setReceiveTime(o.getReceiveTime());
            vo.setLogisticsCompany(o.getLogisticsCompany());
            vo.setLogisticsNo(o.getLogisticsNo());
            vo.setExpireTime(o.getExpireTime());
            vo.setPayStatus(o.getPayStatus());
            return vo;
        }).toList();

        Page<OrderVO> result = new Page<>(c, s);
        result.setTotal(orderPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public OrderVO getDetail(Long id) {
        Order order = getOrder(id);

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
        vo.setUpdateTime(order.getUpdateTime());

        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        }

        Address address = null;
        if (order.getAddressId() != null) {
            address = addressMapper.selectById(order.getAddressId());
        }
        if (address != null) {
            OrderVO.AddressSnapshot snap = new OrderVO.AddressSnapshot();
            snap.setReceiverName(address.getReceiverName());
            snap.setReceiverPhone(address.getReceiverPhone());
            snap.setProvince(address.getProvince());
            snap.setCity(address.getCity());
            snap.setDistrict(address.getDistrict());
            snap.setDetailAddress(address.getDetailAddress());
            vo.setAddressSnapshot(snap);
        }

        List<OrderItem> orderItems = itemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", id)
        );
        if (!orderItems.isEmpty()) {
            List<Long> productIds = orderItems.stream()
                    .map(OrderItem::getProductId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

            List<OrderVO.OrderItemVO> itemVOs = orderItems.stream().map(oi -> {
                OrderVO.OrderItemVO iv = new OrderVO.OrderItemVO();
                iv.setId(oi.getId());
                iv.setProductId(oi.getProductId());
                iv.setSkuId(oi.getSkuId());
                iv.setProductName(oi.getProductName());
                iv.setSkuInfo(oi.getSkuInfo());
                iv.setPrice(oi.getPrice());
                iv.setQuantity(oi.getQuantity());
                iv.setSubtotal(oi.getSubtotal());

                Product product = productMap.get(oi.getProductId());
                if (product != null) {
                    iv.setProductImage(product.getMainImage());
                }
                return iv;
            }).toList();
            vo.setItems(itemVOs);
        }

        vo.setTimeline(buildTimeline(order));
        return vo;
    }

    private List<OrderVO.OrderTimelineVO> buildTimeline(Order order) {
        List<OrderVO.OrderTimelineVO> timeline = new java.util.ArrayList<>();
        int status = order.getOrderStatus();

        addTimeline(timeline, 0, "创建订单", order.getCreateTime());
        if (status >= 1) addTimeline(timeline, 1, "支付成功", order.getPayTime());
        if (status >= 2 && status != 5 && status != 6) {
            String desc = order.getLogisticsCompany() != null
                    ? order.getLogisticsCompany() + (order.getLogisticsNo() != null ? " " + order.getLogisticsNo() : "")
                    : null;
            addTimeline(timeline, 2, "商品出库", order.getShipTime(), desc);
        }
        if (status >= 3 && status != 5 && status != 6) addTimeline(timeline, 3, "确认收货", order.getReceiveTime());
        if (status == 4) addTimeline(timeline, 4, "订单取消", order.getCreateTime());
        if (status == 5) addTimeline(timeline, 5, "退款中", order.getUpdateTime());
        if (status == 6) addTimeline(timeline, 6, "已退款", order.getUpdateTime());

        return timeline;
    }

    private void addTimeline(List<OrderVO.OrderTimelineVO> timeline, int status, String text, LocalDateTime time) {
        addTimeline(timeline, status, text, time, null);
    }

    private void addTimeline(List<OrderVO.OrderTimelineVO> timeline, int status, String text, LocalDateTime time, String desc) {
        OrderVO.OrderTimelineVO t = new OrderVO.OrderTimelineVO();
        t.setStatus(status);
        t.setStatusText(text);
        t.setTime(time);
        t.setDescription(desc);
        timeline.add(t);
    }

    @Override
    public List<OrderItem> items(Long id) {
        return itemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", id));
    }

    @Override
    public void ship(Long id, String logisticsCompany, String logisticsNo) {
        Order o = getOrder(id);
        if (o.getOrderStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态错误，无法发货");
        }
        o.setOrderStatus(2);
        o.setShipTime(LocalDateTime.now());
        o.setLogisticsCompany(logisticsCompany);
        o.setLogisticsNo(logisticsNo);
        o.setAutoConfirmDeadline(LocalDateTime.now().plusDays(7));
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);

        // 发货后发送站内消息通知
        String logisticsInfo = logisticsCompany != null
                ? logisticsCompany + (logisticsNo != null ? " " + logisticsNo : "")
                : "";
        notificationService.notify(
                o.getUserId(),
                "ORDER_SHIPPED",
                "商品已发货",
                "您的订单 " + o.getOrderNo() + " 已发货" + (logisticsInfo.isEmpty() ? "" : "，物流信息：" + logisticsInfo),
                "ORDER",
                o.getId()
        );
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Order o = getOrder(id);
        o.setOrderStatus(status);
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);
    }
}
