package com.mall.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.Coupon;
import com.mall.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@Tag(name = "优惠券管理", description = "后台优惠券管理接口")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/page")
    @Operation(summary = "优惠券分页列表", description = "获取优惠券分页列表")
    public Result<PageResult<Coupon>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<Coupon> page = couponService.page(current, size, keyword);
        PageResult<Coupon> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "优惠券详情", description = "获取优惠券详情")
    public Result<Coupon> getById(@PathVariable Long id) {
        Coupon coupon = couponService.getById(id);
        return Result.success(coupon);
    }

    @PostMapping
    @Operation(summary = "新增优惠券", description = "新增优惠券")
    public Result<Void> save(@RequestBody Coupon coupon) {
        couponService.save(coupon);
        return Result.success("新增成功", null);
    }

    @PutMapping
    @Operation(summary = "更新优惠券", description = "更新优惠券信息")
    public Result<Void> update(@RequestBody Coupon coupon) {
        couponService.update(coupon);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除优惠券", description = "删除优惠券")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.deleteById(id);
        return Result.success("删除成功", null);
    }
}