package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.service.StockSubscriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-subscriptions")
public class StockSubscriptionController {
    private final StockSubscriptionService service;

    public StockSubscriptionController(StockSubscriptionService service) {
        this.service = service;
    }

    @GetMapping
    public Result<java.util.List<com.mall.vo.StockSubscriptionVO>> list(@AuthenticationPrincipal Long userId) {
        return Result.success(service.list(userId));
    }

    @PostMapping
    public Result<Void> subscribe(@AuthenticationPrincipal Long userId, @RequestBody SubscriptionRequest r) {
        service.subscribe(userId, r.productId(), r.skuId());
        return Result.success("已订阅补货提醒", null);
    }

    @DeleteMapping
    public Result<Void> unsubscribe(@AuthenticationPrincipal Long userId, @RequestParam Long productId, @RequestParam(required = false) Long skuId) {
        service.unsubscribe(userId, productId, skuId);
        return Result.success("已取消订阅", null);
    }

    public record SubscriptionRequest(Long productId, Long skuId) {
    }
}
