package com.mall.service;

import com.mall.entity.SeckillRequest;
import com.mall.vo.MarketingParticipateVO;

public interface SeckillAsyncService {
    MarketingParticipateVO submit(Long userId, Long activityId, Long itemId, Integer quantity);
    SeckillRequest getRequest(Long userId, String requestId);
    void process(String requestId);
    void failAndCompensate(String requestId, String reason);
    void restoreActivityStock(Long itemId, Long userId, Integer quantity);
}
