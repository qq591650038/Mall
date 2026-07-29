package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.service.CartService;
import com.mall.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "购物车", description = "购物车接口")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取购物车列表", description = "获取当前用户的购物车商品列表")
    public Result<List<CartVO>> list(@AuthenticationPrincipal Long userId) {
        List<CartVO> list = cartService.listByUserId(userId);
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "加入购物车", description = "将商品SKU加入购物车")
    public Result<Void> add(@AuthenticationPrincipal Long userId,
                            @RequestBody CartAddRequest request) {
        cartService.add(userId, request.getSkuId(), request.getQuantity());
        return Result.success("加入成功", null);
    }

    @PutMapping("/{id}/quantity")
    @Operation(summary = "修改数量", description = "修改购物车商品数量")
    public Result<Void> updateQuantity(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long id,
                                        @RequestBody CartUpdateRequest request) {
        cartService.updateQuantity(id, userId, request.getQuantity());
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除购物车商品", description = "删除购物车中的商品")
    public Result<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        cartService.delete(id, userId);
        return Result.success("删除成功", null);
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除", description = "批量删除购物车商品")
    public Result<Void> batchDelete(@AuthenticationPrincipal Long userId,
                                    @RequestBody List<Long> cartIds) {
        cartService.batchDelete(cartIds, userId);
        return Result.success("删除成功", null);
    }

    @PutMapping("/select-all")
    @Operation(summary = "全选/取消全选", description = "全选或取消全选购物车商品")
    public Result<Void> selectAll(@AuthenticationPrincipal Long userId,
                                   @RequestParam Boolean selected) {
        cartService.selectAll(userId, selected);
        return Result.success("操作成功", null);
    }

    @lombok.Data
    public static class CartAddRequest {
        private Long skuId;
        private Integer quantity;
    }

    @lombok.Data
    public static class CartUpdateRequest {
        private Integer quantity;
    }
}
