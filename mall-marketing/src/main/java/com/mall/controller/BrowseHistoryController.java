package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.BrowseHistory;
import com.mall.service.BrowseHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/browse-history")
@Tag(name = "浏览历史", description = "浏览历史接口")
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    public BrowseHistoryController(BrowseHistoryService browseHistoryService) {
        this.browseHistoryService = browseHistoryService;
    }

    @GetMapping("/list")
    @Operation(summary = "浏览历史列表", description = "获取当前用户的浏览历史")
    public Result<List<BrowseHistory>> list(@AuthenticationPrincipal Long userId) {
        List<BrowseHistory> list = browseHistoryService.listByUserId(userId);
        return Result.success(list);
    }

    @PostMapping("/product/{productId}")
    @Operation(summary = "记录浏览", description = "记录商品浏览历史")
    public Result<Void> add(@AuthenticationPrincipal Long userId, @PathVariable Long productId) {
        browseHistoryService.add(userId, productId);
        return Result.success("记录成功", null);
    }

    @DeleteMapping("/product/{productId}")
    @Operation(summary = "删除浏览记录", description = "删除单条浏览记录")
    public Result<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long productId) {
        browseHistoryService.delete(userId, productId);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空浏览历史", description = "清空所有浏览历史")
    public Result<Void> clear(@AuthenticationPrincipal Long userId) {
        browseHistoryService.clear(userId);
        return Result.success("清空成功", null);
    }
}