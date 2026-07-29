package com.mall.service;

import com.mall.entity.PointsProduct;

public interface PointsRewardService {
    void validateConfiguration(PointsProduct product);

    FulfillmentResult fulfill(Long userId, PointsProduct product, Long addressId);

    record FulfillmentResult(Long orderId, Long userCouponId, String fulfillmentStatus) {
    }
}
