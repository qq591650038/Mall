package com.mall.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.Brand;
import com.mall.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/brands")
@Tag(name = "品牌管理", description = "后台品牌管理接口")
public class AdminBrandController {

    private final BrandService brandService;

    public AdminBrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping("/page")
    @Operation(summary = "品牌分页列表", description = "获取品牌分页列表")
    public Result<PageResult<Brand>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<Brand> page = brandService.page(current, size, keyword);
        PageResult<Brand> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "品牌详情", description = "获取品牌详情")
    public Result<Brand> getById(@PathVariable Long id) {
        Brand brand = brandService.getById(id);
        return Result.success(brand);
    }

    @PostMapping
    @Operation(summary = "新增品牌", description = "新增品牌")
    public Result<Void> save(@RequestBody Brand brand) {
        brandService.save(brand);
        return Result.success("新增成功", null);
    }

    @PutMapping
    @Operation(summary = "更新品牌", description = "更新品牌信息")
    public Result<Void> update(@RequestBody Brand brand) {
        brandService.update(brand);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除品牌", description = "删除品牌")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.deleteById(id);
        return Result.success("删除成功", null);
    }
}