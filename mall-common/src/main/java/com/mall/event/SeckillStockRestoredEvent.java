package com.mall.event;

public record SeckillStockRestoredEvent(Long activityId, Long itemId, Long userId, Integer quantity) {
}
