package com.mall.service;

import com.mall.entity.Address;
import com.mall.entity.ShippingTemplate;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingService {
    BigDecimal calculate(Long templateId, String deliveryMethod, Address address, BigDecimal goodsAmount);
    List<ShippingTemplate> available();
    void save(ShippingTemplate template);
    void remove(Long id);
}
