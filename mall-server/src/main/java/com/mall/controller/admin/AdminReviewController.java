package com.mall.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.Review;
import com.mall.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
@Tag(name = "评价管理", description = "后台评价管理接口")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/page")
    @Operation(summary = "评价分页列表", description = "分页获取评价，支持按评分和状态筛选")
    public Result<PageResult<Review>> page(@RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) Integer rating,
                                            @RequestParam(required = false) Integer status) {
        Page<Review> page = reviewService.pageForAdmin(current, size, rating, status);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), current, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "评价详情", description = "获取评价详情")
    public Result<Review> getById(@PathVariable Long id) {
        Review review = reviewService.getById(id);
        return Result.success(review);
    }

    @PutMapping("/{id}/reply")
    @Operation(summary = "回复评价", description = "管理员回复评价")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewService.reply(id, body.get("reply"));
        return Result.success("回复成功", null);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新评价状态", description = "隐藏或显示评价")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        reviewService.updateStatus(id, body.get("status"));
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评价", description = "删除评价")
    public Result<Void> delete(@PathVariable Long id) {
        reviewService.deleteById(id);
        return Result.success("删除成功", null);
    }
}
