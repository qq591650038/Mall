package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.SeckillRequest;
import com.mall.service.SeckillAsyncService;
import com.mall.vo.MarketingParticipateVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketing/seckill")
public class SeckillOrderController {
    private final SeckillAsyncService seckillAsyncService;

    public SeckillOrderController(SeckillAsyncService seckillAsyncService) {
        this.seckillAsyncService = seckillAsyncService;
    }

    @PostMapping("/participate")
    public Result<MarketingParticipateVO> submit(@AuthenticationPrincipal Long userId, @RequestBody Request request) {
        return Result.success(seckillAsyncService.submit(userId, request.activityId, request.itemId, request.quantity));
    }

    @GetMapping("/requests/{requestId}")
    public Result<SeckillRequest> status(@AuthenticationPrincipal Long userId, @PathVariable String requestId) {
        return Result.success(seckillAsyncService.getRequest(userId, requestId));
    }

    public static class Request {
        public Long activityId;
        public Long itemId;
        public Integer quantity;
    }
}
