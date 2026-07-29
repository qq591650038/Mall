package com.mall.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.dto.order.CreateOrderDTO;
import com.mall.vo.OrderVO;
import com.mall.vo.PayResultVO;
import java.math.BigDecimal;
import java.util.Map;
public interface OrderService {
    OrderVO createOrder(Long userId, CreateOrderDTO dto);
    OrderVO createMarketingOrder(Long userId, CreateOrderDTO dto, Map<Long, BigDecimal> activityPrices);
    OrderVO getById(Long id, Long userId);
    Page<OrderVO> pageByUserId(Long userId, Integer current, Integer size, Integer orderStatus);
    void cancelOrder(Long id, Long userId);
    PayResultVO payOrder(Long id, Long userId);
    void confirmReceive(Long id, Long userId);
    void onPaymentSuccess(Long orderId);
    void onOrderCancel(Long orderId);
}
