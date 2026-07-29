package com.mall.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.CursorPageResult;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.service.RefundService;
import com.mall.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/refunds")
@Tag(name = "退款管理", description = "后台退款管理接口")
public class AdminRefundController {

    private final RefundService refundService;

    public AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping("/page")
    @Operation(summary = "退款列表", description = "获取所有退款记录列表")
    public Result<PageResult<RefundVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {
        Page<RefundVO> page = refundService.pageAdmin(current, size, status, orderNo);
        PageResult<RefundVO> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @GetMapping("/cursor")
    public Result<CursorPageResult<RefundVO>> cursorPage(
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String cursor) {
        return Result.success(refundService.cursorPageAdmin(size, status, orderNo, cursor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "退款详情", description = "获取退款详情")
    public Result<RefundVO> getById(@PathVariable Long id) {
        RefundVO vo = refundService.getById(id, null);
        return Result.success(vo);
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "审核退款", description = "审核通过或拒绝退款")
    public Result<Void> review(@PathVariable Long id,
                               @RequestBody Map<String, Object> body) {
        Integer status = (Integer) body.get("status");
        String remark = (String) body.get("remark");
        refundService.review(id, status, remark);
        return Result.success("审核成功", null);
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "手动完成退款", description = "仅供未接入支付渠道时测试退款成功流程")
    public Result<Void> complete(@PathVariable Long id) {
        refundService.refundSuccess(id);
        return Result.success("退款完成", null);
    }

    @PostMapping("/{id}/fail")
    public Result<Void> fail(@PathVariable Long id, @RequestBody java.util.Map<String,String> body) {
        refundService.refundFailed(id, body.getOrDefault("reason", "第三方退款失败")); return Result.success("已记录失败", null);
    }

    @PutMapping("/{id}/return-logistics")
    @Operation(summary = "更新退货物流", description = "管理员更新退货物流信息")
    public Result<Void> updateReturnLogistics(@PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        String logisticsCompany = body.get("logisticsCompany");
        String logisticsNo = body.get("logisticsNo");
        refundService.updateReturnLogistics(id, logisticsCompany, logisticsNo);
        return Result.success("更新成功", null);
    }

    @PutMapping("/{id}/exchange-logistics")
    @Operation(summary = "更新换货发出物流", description = "管理员发出换货新品时更新物流单号")
    public Result<Void> updateExchangeLogistics(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body) {
        String trackingNo = body.get("trackingNo");
        refundService.updateExchangeLogistics(id, trackingNo);
        return Result.success("更新成功", null);
    }
}
