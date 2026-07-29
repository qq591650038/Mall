package com.mall.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.Coupon;
import com.mall.entity.UserCoupon;
import com.mall.vo.UserCouponVO;
import java.util.List;
public interface CouponService {
    Page<Coupon> page(Integer current, Integer size, String keyword);
    Coupon getById(Long id);
    void save(Coupon coupon);
    void update(Coupon coupon);
    void deleteById(Long id);
    List<Coupon> listAvailable(Long userId);
    List<UserCouponVO> listByUserId(Long userId);
    List<UserCouponVO> listUsableByUserId(Long userId);
    UserCoupon receive(Long userId, Long couponId);
    UserCoupon grantForPoints(Long userId, Long couponId);
    void lockCoupon(Long userCouponId, Long orderId);
    void releaseCoupon(Long userCouponId);
    void recordUsageEvent(Long userCouponId, String eventType, Long orderId, String remark);
}
