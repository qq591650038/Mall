package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.Coupon;
import com.mall.entity.UserCoupon;
import com.mall.service.CouponService;
import com.mall.vo.UserCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "优惠券", description = "优惠券接口")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/available")
    @Operation(summary = "获取可用优惠券", description = "获取当前可领取的优惠券列表")
    public Result<List<Coupon>> listAvailable(@AuthenticationPrincipal Long userId) {
        List<Coupon> list = couponService.listAvailable(userId);
        return Result.success(list);
    }

    @GetMapping("/usable")
    @Operation(summary = "我的可用优惠券", description = "获取当前用户可用于结算的优惠券列表")
    public Result<List<UserCouponVO>> listUsable(@AuthenticationPrincipal Long userId) {
        List<UserCouponVO> list = couponService.listUsableByUserId(userId);
        return Result.success(list);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "领取优惠券", description = "领取指定优惠券")
    public Result<UserCoupon> receive(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        UserCoupon userCoupon = couponService.receive(userId, id);
        return Result.success("领取成功", userCoupon);
    }

    @GetMapping("/mine")
    @Operation(summary = "我的优惠券", description = "获取当前用户的优惠券列表")
    public Result<List<UserCouponVO>> listMine(@AuthenticationPrincipal Long userId) {
        List<UserCouponVO> list = couponService.listByUserId(userId);
        return Result.success(list);
    }
}
