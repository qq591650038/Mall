package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.service.OrderService;
import com.mall.vo.OrderVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {
    private final OrderService orders;

    public LogisticsController(OrderService o) {
        orders = o;
    }

    @GetMapping("/{orderId}")
    public Result<OrderVO> get(@AuthenticationPrincipal Long userId, @PathVariable Long orderId) {
        return Result.success(orders.getById(orderId, userId));
    }
}
