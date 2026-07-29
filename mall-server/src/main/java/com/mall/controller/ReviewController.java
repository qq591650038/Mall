package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.Review;
import com.mall.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "商品评价", description = "商品评价接口")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/product/{productId}/summary")
    @Operation(summary = "商品评价统计", description = "获取商品评价总数、好评数和平均星级")
    public Result<Map<String, Object>> summaryByProduct(@PathVariable Long productId) {
        return Result.success(reviewService.summaryByProductId(productId));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "商品评价列表", description = "获取商品的评价列表")
    public Result<PageResult<Review>> listByProduct(@PathVariable Long productId,
                                                     @RequestParam(defaultValue = "1") Integer current,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @RequestParam(required = false) Integer ratingType,
                                                     @RequestParam(required = false) Boolean hasImages) {
        Page<Review> page = reviewService.pageByProductId(productId, current, size, ratingType, hasImages);
        PageResult<Review> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @GetMapping("/mine")
    @Operation(summary = "我的评价列表", description = "获取当前用户的评价列表")
    public Result<PageResult<Review>> listMine(@AuthenticationPrincipal Long userId,
                                                 @RequestParam(defaultValue = "1") Integer current,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        Page<Review> page = reviewService.pageByUserId(userId, current, size);
        PageResult<Review> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @PostMapping
    @Operation(summary = "发表评价", description = "对商品发表评价")
    public Result<Void> save(@AuthenticationPrincipal Long userId, @RequestBody Review review) {
        review.setUserId(userId);
        reviewService.save(review);
        return Result.success("评价成功", null);
    }
}
