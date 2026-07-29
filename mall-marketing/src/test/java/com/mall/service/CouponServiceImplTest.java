package com.mall.service;

import com.mall.entity.UserCoupon;
import com.mall.entity.Coupon;
import com.mall.mapper.CouponAuditLogMapper;
import com.mall.mapper.CouponMapper;
import com.mall.mapper.UserCouponMapper;
import com.mall.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CouponServiceImplTest {
    private final CouponMapper couponMapper = mock(CouponMapper.class);
    private final UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
    private final CouponAuditLogMapper auditLogMapper = mock(CouponAuditLogMapper.class);
    private final CouponServiceImpl service = new CouponServiceImpl(couponMapper, userCouponMapper, auditLogMapper);

    @Test
    void lockCouponRejectsAlreadyLockedCoupon() {
        when(userCouponMapper.selectById(1L)).thenReturn(new UserCoupon());
        when(userCouponMapper.lock(1L, null, 99L)).thenReturn(0);
        assertThrows(RuntimeException.class, () -> service.lockCoupon(1L, 99L));
    }

    @Test
    void releaseCouponDelegatesToCompensationMapper() {
        UserCoupon coupon = new UserCoupon();
        coupon.setCouponId(2L);
        when(userCouponMapper.selectById(1L)).thenReturn(coupon);
        when(userCouponMapper.unlock(1L)).thenReturn(1);
        service.releaseCoupon(1L);
        verify(userCouponMapper).unlock(1L);
        verify(couponMapper).incrementRemain(2L);
    }

    @Test
    void availableCouponsExcludeCouponsAlreadyReceivedByUser() {
        Coupon received = new Coupon();
        received.setId(1L);
        Coupon notReceived = new Coupon();
        notReceived.setId(2L);
        when(couponMapper.selectList(any())).thenReturn(java.util.List.of(received, notReceived));
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setCouponId(1L);
        when(userCouponMapper.selectList(any())).thenReturn(java.util.List.of(userCoupon));

        assertEquals(java.util.List.of(notReceived), service.listAvailable(7L));
    }
}
