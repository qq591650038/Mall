package com.mall.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.Product;
import com.mall.service.ProductService;
import com.mall.vo.ProductDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "商品管理", description = "后台商品管理接口")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/page")
    @Operation(summary = "商品分页列表", description = "获取所有商品分页列表")
    public Result<PageResult<Product>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        Page<Product> page = productService.page(current, size, keyword, categoryId, status);
        PageResult<Product> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情", description = "获取商品详情")
    public Result<ProductDetailVO> getDetail(@PathVariable Long id) {
        ProductDetailVO detail = productService.getDetail(id);
        return Result.success(detail);
    }

    @PostMapping
    @Operation(summary = "新增商品", description = "新增商品")
    public Result<Void> save(@RequestBody Product product) {
        productService.save(product);
        return Result.success("新增成功", null);
    }

    @PutMapping
    @Operation(summary = "更新商品", description = "更新商品信息")
    public Result<Void> update(@RequestBody Product product) {
        productService.update(product);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品", description = "删除商品及其SKU和图片")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteById(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/on-shelf")
    @Operation(summary = "上架商品", description = "将商品上架")
    public Result<Void> onShelf(@PathVariable Long id) {
        productService.onShelf(id);
        return Result.success("上架成功", null);
    }

    @PutMapping("/{id}/off-shelf")
    @Operation(summary = "下架商品", description = "将商品下架")
    public Result<Void> offShelf(@PathVariable Long id) {
        productService.offShelf(id);
        return Result.success("下架成功", null);
    }
}