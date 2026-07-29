package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.FavoriteGroup;
import com.mall.service.FavoriteGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏分组控制器
 */
@RestController
@RequestMapping("/api/favorite-groups")
@Tag(name = "收藏分组", description = "收藏分组管理接口")
public class FavoriteGroupController {

    private final FavoriteGroupService favoriteGroupService;

    public FavoriteGroupController(FavoriteGroupService favoriteGroupService) {
        this.favoriteGroupService = favoriteGroupService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取分组列表", description = "获取当前用户的所有收藏分组")
    public Result<List<FavoriteGroup>> list(@AuthenticationPrincipal Long userId) {
        List<FavoriteGroup> list = favoriteGroupService.listByUserId(userId);
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "创建分组", description = "创建新的收藏分组")
    public Result<FavoriteGroup> create(@AuthenticationPrincipal Long userId,
                                         @RequestBody CreateGroupRequest request) {
        FavoriteGroup group = favoriteGroupService.create(userId, request.getName(), request.getSort());
        return Result.success(group);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分组", description = "更新收藏分组名称和排序")
    public Result<FavoriteGroup> update(@PathVariable Long id,
                                         @RequestBody CreateGroupRequest request) {
        FavoriteGroup group = favoriteGroupService.update(id, request.getName(), request.getSort());
        return Result.success(group);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分组", description = "删除收藏分组，分组下的收藏将移至未分组")
    public Result<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        favoriteGroupService.delete(id, userId);
        return Result.success("删除成功", null);
    }

    /** 创建分组请求体 */
    public static class CreateGroupRequest {
        private String name;
        private Integer sort;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }
    }
}