package com.mall.service;
/** 订单流程使用的库存边界，具体库存持久化实现由订单模块负责。 */
public interface InventoryService { void reserve(Long skuId, Long productId, int quantity); void release(Long skuId, Long productId, int quantity); }
