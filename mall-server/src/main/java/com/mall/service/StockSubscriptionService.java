package com.mall.service;
import com.mall.vo.StockSubscriptionVO;
import java.util.List;
public interface StockSubscriptionService { void subscribe(Long userId, Long productId, Long skuId); void unsubscribe(Long userId, Long productId, Long skuId); List<StockSubscriptionVO> list(Long userId); void notifyRestored(Long productId, Long skuId); }
