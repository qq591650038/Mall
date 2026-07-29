package com.mall.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.OrderItem;
import com.mall.service.AdminOrderService;
import com.mall.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "订单管理", description = "后台订单管理接口")
public class AdminOrderController {

    private final AdminOrderService orderService;

    public AdminOrderController(AdminOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/page")
    @Operation(summary = "订单分页列表", description = "获取所有订单分页列表")
    public Result<PageResult<OrderVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId) {
        Page<OrderVO> orderPage = orderService.page(current,size,orderStatus,orderNo,userId);
        PageResult<OrderVO> result = new PageResult<>(orderPage.getTotal(), orderPage.getRecords(), current, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情", description = "获取订单详情，包含用户信息、商品明细、时间线")
    public Result<OrderVO> getById(@PathVariable Long id) {
        OrderVO vo = orderService.getDetail(id);
        return Result.success(vo);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "订单明细", description = "获取订单明细列表")
    public Result<List<OrderItem>> getItems(@PathVariable Long id) {
        List<OrderItem> items = orderService.items(id);
        return Result.success(items);
    }

    @PutMapping("/{id}/ship")
    @Operation(summary = "订单发货", description = "将订单状态改为已发货")
    public Result<Void> ship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String logisticsCompany = body.get("logisticsCompany");
        String logisticsNo = body.get("logisticsNo");
        orderService.ship(id, logisticsCompany, logisticsNo);
        return Result.success("发货成功", null);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改订单状态", description = "修改订单状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        orderService.updateStatus(id, body.get("orderStatus"));
        return Result.success("修改成功", null);
    }
}
