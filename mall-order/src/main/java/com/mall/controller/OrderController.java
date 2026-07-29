package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.dto.order.CreateOrderDTO;
import com.mall.dto.order.PaymentCallbackDTO;
import com.mall.service.PaymentService;
import com.mall.service.OrderService;
import com.mall.vo.OrderVO;
import com.mall.vo.PayResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单", description = "订单管理接口")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "创建订单", description = "创建新订单，从购物车结算")
    public Result<OrderVO> create(@AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody CreateOrderDTO dto) {
        OrderVO vo = orderService.createOrder(userId, dto);
        return Result.success("创建成功", vo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情", description = "获取订单详情")
    public Result<OrderVO> getById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        OrderVO vo = orderService.getById(id, userId);
        return Result.success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "订单列表", description = "获取当前用户的订单列表")
    public Result<PageResult<OrderVO>> page(@AuthenticationPrincipal Long userId,
                                            @RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) Integer orderStatus) {
        Page<OrderVO> page = orderService.pageByUserId(userId, current, size, orderStatus);
        PageResult<OrderVO> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "取消待支付订单")
    public Result<Void> cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        orderService.cancelOrder(id, userId);
        return Result.success("取消成功", null);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "支付订单", description = "发起支付，返回支付单号和支付信息")
    public Result<PayResultVO> pay(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        PayResultVO vo = orderService.payOrder(id, userId);
        return Result.success("支付成功", vo);
    }

    @PostMapping("/payment/callback")
    public Result<Void> paymentCallback(@Valid @RequestBody PaymentCallbackDTO callback) {
        paymentService.callback(callback);
        return Result.success("回调成功", null);
    }

    @PostMapping("/{id}/payment/mock-confirm")
    public Result<Void> mockPaymentConfirm(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long id,
                                           @RequestParam String paymentNo) {
        paymentService.confirmMockPayment(userId, id, paymentNo);
        return Result.success("支付成功", null);
    }

    @PostMapping("/{id}/confirm-receive")
    @Operation(summary = "确认收货", description = "确认已收到商品")
    public Result<Void> confirmReceive(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        orderService.confirmReceive(id, userId);
        return Result.success("确认成功", null);
    }
}
