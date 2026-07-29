package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.Refund;
import com.mall.dto.order.RefundCallbackDTO;
import com.mall.service.RefundService;
import com.mall.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/refunds")
@Tag(name = "退款管理", description = "用户退款接口")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    @Operation(summary = "申请退款", description = "用户申请订单退款")
    public Result<RefundVO> apply(@AuthenticationPrincipal Long userId,
                                   @RequestBody Refund refund) {
        RefundVO vo = refundService.apply(userId, refund.getOrderId(), refund);
        return Result.success("申请成功", vo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "退款详情", description = "获取退款详情")
    public Result<RefundVO> getById(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long id) {
        RefundVO vo = refundService.getById(id, userId);
        return Result.success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "退款列表", description = "获取当前用户的退款列表")
    public Result<PageResult<RefundVO>> page(@AuthenticationPrincipal Long userId,
                                              @RequestParam(defaultValue = "1") Integer current,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              @RequestParam(required = false) Integer status) {
        Page<RefundVO> page = refundService.pageByUserId(userId, current, size, status);
        PageResult<RefundVO> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "撤销退款", description = "撤销待审核退款申请")
    public Result<Void> cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        refundService.cancel(id, userId); return Result.success("已撤销", null);
    }

    @PostMapping("/callback")
    public Result<Void> callback(@jakarta.validation.Valid @RequestBody RefundCallbackDTO callback) {
        refundService.callback(callback);
        return Result.success("退款回调成功", null);
    }

    @PostMapping("/{id}/return-logistics")
    @Operation(summary = "提交退货物流", description = "用户提交退货物流信息")
    public Result<Void> submitReturnLogistics(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        String logisticsCompany = body.get("logisticsCompany");
        String logisticsNo = body.get("logisticsNo");
        refundService.submitReturnLogistics(userId, id, logisticsCompany, logisticsNo);
        return Result.success("提交成功", null);
    }

    @PostMapping("/exchange")
    @Operation(summary = "申请换货", description = "用户申请换货")
    public Result<RefundVO> applyExchange(@AuthenticationPrincipal Long userId,
                                           @RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Long exchangeProductId = Long.valueOf(body.get("exchangeProductId").toString());
        Long exchangeSkuId = body.get("exchangeSkuId") != null
                ? Long.valueOf(body.get("exchangeSkuId").toString()) : null;
        String reason = (String) body.get("reason");
        RefundVO vo = refundService.applyExchange(userId, orderId, exchangeProductId, exchangeSkuId, reason);
        return Result.success("申请成功", vo);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认收货", description = "用户确认退货/换货流程完成")
    public Result<Void> confirmReturnReceived(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long id) {
        refundService.confirmReturnReceived(userId, id);
        return Result.success("确认成功", null);
    }
}