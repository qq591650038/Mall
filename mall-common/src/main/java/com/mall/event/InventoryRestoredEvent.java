package com.mall.event;

public record InventoryRestoredEvent(Long productId, Long skuId, Integer quantity) {
}
