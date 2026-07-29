package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.Favorite;
import com.mall.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收藏控制器
 */
@RestController
@RequestMapping("/api/favorites")
@Tag(name = "收藏", description = "商品收藏接口")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/list")
    @Operation(summary = "收藏列表", description = "获取当前用户的所有收藏")
    public Result<List<Favorite>> list(@AuthenticationPrincipal Long userId) {
        List<Favorite> list = favoriteService.listByUserId(userId);
        return Result.success(list);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "按分组获取收藏", description = "获取指定分组下的收藏")
    public Result<List<Favorite>> listByGroup(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long groupId) {
        List<Favorite> list = favoriteService.listByGroupId(userId, groupId);
        return Result.success(list);
    }

    @GetMapping("/ungrouped")
    @Operation(summary = "获取未分组收藏", description = "获取未分组的收藏列表")
    public Result<List<Favorite>> listUngrouped(@AuthenticationPrincipal Long userId) {
        List<Favorite> list = favoriteService.listUngrouped(userId);
        return Result.success(list);
    }

    @PostMapping("/product/{productId}")
    @Operation(summary = "收藏商品", description = "收藏商品（可选分组和价格记录）")
    public Result<Void> add(@AuthenticationPrincipal Long userId,
                             @PathVariable Long productId,
                             @RequestBody(required = false) AddFavoriteRequest request) {
        Long groupId = null;
        BigDecimal originalPrice = null;
        if (request != null) {
            groupId = request.getGroupId();
            originalPrice = request.getOriginalPrice();
        }
        favoriteService.add(userId, productId, groupId, originalPrice);
        return Result.success("收藏成功", null);
    }

    @DeleteMapping("/product/{productId}")
    @Operation(summary = "取消收藏", description = "取消收藏商品")
    public Result<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long productId) {
        favoriteService.delete(userId, productId);
        return Result.success("取消成功", null);
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "检查是否已收藏", description = "检查商品是否已收藏")
    public Result<Boolean> check(@AuthenticationPrincipal Long userId, @PathVariable Long productId) {
        Boolean isFavorite = favoriteService.isFavorite(userId, productId);
        return Result.success(isFavorite);
    }

    @PutMapping("/product/{productId}/group")
    @Operation(summary = "更新收藏分组", description = "更新收藏的分组")
    public Result<Void> updateGroup(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long productId,
                                     @RequestBody UpdateGroupRequest request) {
        favoriteService.updateGroup(userId, productId, request.getGroupId());
        return Result.success("更新成功", null);
    }

    @PutMapping("/product/{productId}/price-alert")
    @Operation(summary = "更新降价提醒", description = "开启或关闭降价提醒")
    public Result<Void> updatePriceAlert(@AuthenticationPrincipal Long userId,
                                          @PathVariable Long productId,
                                          @RequestBody UpdateAlertRequest request) {
        favoriteService.updatePriceAlert(userId, productId, request.getEnabled());
        return Result.success("更新成功", null);
    }

    @PutMapping("/product/{productId}/stock-alert")
    @Operation(summary = "更新到货提醒", description = "开启或关闭到货提醒")
    public Result<Void> updateStockAlert(@AuthenticationPrincipal Long userId,
                                          @PathVariable Long productId,
                                          @RequestBody UpdateAlertRequest request) {
        favoriteService.updateStockAlert(userId, productId, request.getEnabled());
        return Result.success("更新成功", null);
    }

    /** 添加收藏请求体 */
    public static class AddFavoriteRequest {
        private Long groupId;
        private BigDecimal originalPrice;

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }

        public BigDecimal getOriginalPrice() {
            return originalPrice;
        }

        public void setOriginalPrice(BigDecimal originalPrice) {
            this.originalPrice = originalPrice;
        }
    }

    /** 更新分组请求体 */
    public static class UpdateGroupRequest {
        private Long groupId;

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }
    }

    /** 更新提醒请求体 */
    public static class UpdateAlertRequest {
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}