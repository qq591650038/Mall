package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.entity.Coupon;
import com.mall.entity.UserCoupon;
import com.mall.exception.BusinessException;
import com.mall.mapper.CouponMapper;
import com.mall.mapper.UserCouponMapper;
import com.mall.mapper.CouponAuditLogMapper;
import com.mall.entity.CouponAuditLog;
import com.mall.service.CouponService;
import com.mall.vo.UserCouponVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponAuditLogMapper auditLogMapper;

    public CouponServiceImpl(CouponMapper couponMapper, UserCouponMapper userCouponMapper, CouponAuditLogMapper auditLogMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public Page<Coupon> page(Integer current, Integer size, String keyword) {
        Page<Coupon> page = new Page<>(current, size);
        QueryWrapper<Coupon> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like("name", keyword);
        }
        wrapper.orderByDesc("id");
        return couponMapper.selectPage(page, wrapper);
    }

    @Override
    public Coupon getById(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        return coupon;
    }

    @Override
    public void save(Coupon coupon) {
        coupon.setCreateTime(LocalDateTime.now());
        coupon.setUpdateTime(LocalDateTime.now());
        coupon.setDeleted(0);
        couponMapper.insert(coupon);
        log.info("新增优惠券: {}", coupon.getName());
    }

    @Override
    public void update(Coupon coupon) {
        Coupon exist = couponMapper.selectById(coupon.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        coupon.setUpdateTime(LocalDateTime.now());
        couponMapper.updateById(coupon);
        log.info("更新优惠券: id={}", coupon.getId());
    }

    @Override
    public void deleteById(Long id) {
        Coupon exist = couponMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        couponMapper.deleteById(id);
        log.info("删除优惠券: id={}", id);
    }

    @Override
    public List<Coupon> listAvailable(Long userId) {
        List<Coupon> available = couponMapper.selectList(
                new QueryWrapper<Coupon>()
                        .eq("status", 1)
                        .gt("remain_count", 0)
                        .le("start_time", LocalDateTime.now())
                        .ge("end_time", LocalDateTime.now())
                        .orderByDesc("id")
        );
        if (userId == null || available.isEmpty()) {
            return available;
        }

        // 已领取的优惠券不再展示在可领取列表，避免用户重复领取同一张券。
        List<Long> receivedCouponIds = userCouponMapper.selectList(
                        new QueryWrapper<UserCoupon>()
                                .select("coupon_id")
                                .eq("user_id", userId)
                ).stream()
                .map(UserCoupon::getCouponId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .toList();
        return available.stream()
                .filter(coupon -> !receivedCouponIds.contains(coupon.getId()))
                .toList();
    }

    @Override
    public List<UserCouponVO> listByUserId(Long userId) {
        List<UserCoupon> userCoupons = userCouponMapper.selectList(
                new QueryWrapper<UserCoupon>().eq("user_id", userId).orderByDesc("create_time")
        );
        if (userCoupons.isEmpty()) return List.of();
        List<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, coupon -> coupon, (left, right) -> left));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return userCoupons.stream().map(userCoupon -> {
            Coupon coupon = couponMap.get(userCoupon.getCouponId());
            UserCouponVO vo = new UserCouponVO();
            vo.setId(userCoupon.getId());
            vo.setCouponId(userCoupon.getCouponId());
            vo.setStatus(userCoupon.getStatus());
            vo.setReceiveTime(formatTime(userCoupon.getCreateTime(), formatter));
            if (coupon != null) {
                vo.setName(coupon.getName());
                vo.setType(coupon.getType());
                vo.setValue(coupon.getValue());
                vo.setMinAmount(coupon.getMinAmount());
                vo.setStartTime(formatTime(coupon.getStartTime(), formatter));
                vo.setEndTime(formatTime(coupon.getEndTime(), formatter));
            }
            return vo;
        }).toList();
    }

    private String formatTime(LocalDateTime time, DateTimeFormatter formatter) {
        return time == null ? "" : time.format(formatter);
    }

    @Override
    public List<UserCouponVO> listUsableByUserId(Long userId) {
        List<UserCoupon> userCoupons = userCouponMapper.selectList(
                new QueryWrapper<UserCoupon>()
                        .eq("user_id", userId)
                        .eq("status", 0)
                        .orderByDesc("create_time")
        );

        if (userCoupons.isEmpty()) {
            return List.of();
        }

        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c, (a, b) -> a));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        return userCoupons.stream()
                .map(uc -> {
                    Coupon coupon = couponMap.get(uc.getCouponId());
                    if (coupon == null) return null;

                    if (coupon.getStatus() != 1) return null;
                    if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) return null;

                    UserCouponVO vo = new UserCouponVO();
                    vo.setId(uc.getId());
                    vo.setCouponId(coupon.getId());
                    vo.setName(coupon.getName());
                    vo.setType(coupon.getType());
                    vo.setValue(coupon.getValue());
                    vo.setMinAmount(coupon.getMinAmount());
                    vo.setStatus(uc.getStatus());
                    vo.setReceiveTime(uc.getCreateTime() != null ? uc.getCreateTime().format(fmt) : "");
                    vo.setStartTime(formatTime(coupon.getStartTime(), fmt));
                    vo.setEndTime(formatTime(coupon.getEndTime(), fmt));
                    return vo;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon receive(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        if (coupon.getRemainCount() <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已领完");
        }

        Long existCount = userCouponMapper.selectCount(
                new QueryWrapper<UserCoupon>().eq("user_id", userId).eq("coupon_id", couponId)
        );
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "您已领取过该优惠券");
        }

        if (couponMapper.decrementRemain(couponId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已领完或已失效");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        userCoupon.setCreateTime(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);
        recordUsageEvent(userCoupon.getId(), "RECEIVED", null, "用户领取优惠券");

        log.info("领取优惠券: userId={}, couponId={}", userId, couponId);
        return userCoupon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon grantForPoints(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        LocalDateTime now = LocalDateTime.now();
        if (coupon == null || !Objects.equals(coupon.getStatus(), 1)
                || coupon.getStartTime() == null || coupon.getEndTime() == null
                || now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券不存在或不在有效期内");
        }
        if (couponMapper.decrementRemain(couponId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券库存不足");
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        userCoupon.setCreateTime(now);
        userCouponMapper.insert(userCoupon);
        recordUsageEvent(userCoupon.getId(), "POINTS_REDEEMED", null, "积分兑换发放优惠券");
        return userCoupon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券记录不存在");
        }
        if (userCouponMapper.lock(userCouponId, userCoupon.getUserId(), orderId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已被使用或已过期");
        }
        log.info("锁定优惠券: userCouponId={}", userCouponId);
        recordUsageEvent(userCouponId, "LOCKED", orderId, "下单锁定优惠券");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券记录不存在");
        }
        if (userCouponMapper.unlock(userCouponId) == 1) {
            couponMapper.incrementRemain(userCoupon.getCouponId());
            recordUsageEvent(userCouponId, "RELEASED", userCoupon.getOrderId(), "订单取消或支付失败，释放优惠券");
        }
        log.info("释放优惠券: userCouponId={}", userCouponId);
    }

    @Override
    public void recordUsageEvent(Long userCouponId, String eventType, Long orderId, String remark) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) return;
        CouponAuditLog event = new CouponAuditLog();
        event.setUserCouponId(userCoupon.getId());
        event.setUserId(userCoupon.getUserId());
        event.setCouponId(userCoupon.getCouponId());
        event.setOrderId(orderId);
        event.setEventType(eventType);
        event.setRemark(remark);
        event.setCreateTime(LocalDateTime.now());
        event.setModule("CouponService");
        event.setOperation("coupon.lifecycle");
        event.setStatus(1);
        if (auditLogMapper != null) auditLogMapper.insert(event);
    }
}
