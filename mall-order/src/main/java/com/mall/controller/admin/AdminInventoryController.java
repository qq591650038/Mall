package com.mall.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.InventoryLog;
import com.mall.vo.InventoryWarningVO;
import com.mall.service.AdminInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory")
@Tag(name = "库存管理", description = "后台库存管理接口")
public class AdminInventoryController {

    private final AdminInventoryService inventoryService;

    public AdminInventoryController(AdminInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/page")
    @Operation(summary = "库存操作日志", description = "获取库存操作日志分页列表")
    public Result<PageResult<InventoryLog>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String operation) {
        Page<InventoryLog> logPage = inventoryService.page(current, size, operation);
        PageResult<InventoryLog> result = new PageResult<>(logPage.getTotal(), logPage.getRecords(), current, size);
        return Result.success(result);
    }

    @PostMapping("/adjust")
    @Operation(summary = "调整库存", description = "调整商品或SKU库存数量")
    public Result<Void> adjust(@RequestBody Map<String, Object> body) {
        String type=(String)body.get("type"); String action=(String)body.get("action"); Integer quantity=((Number)body.get("quantity")).intValue(); Long productId=body.get("productId")==null?null:Long.valueOf(body.get("productId").toString()); Long skuId=body.get("skuId")==null?null:Long.valueOf(body.get("skuId").toString()); inventoryService.adjust(type,productId,skuId,action,quantity,null);
        return Result.success("调整成功", null);
    }

    @PostMapping("/{id}/retry")
    public Result<Void> retry(@PathVariable Long id) { inventoryService.retry(id); return Result.success("重试成功", null); }

    @GetMapping({"/products/low-stock", "/low-stock"})
    @Operation(summary = "低库存商品", description = "获取库存不足的商品列表")
    public Result<List<InventoryWarningVO>> lowStock(
            @RequestParam(defaultValue = "10") Integer threshold) {
        return Result.success(inventoryService.lowStock(threshold));
    }
}
