package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.Product;
import com.mall.service.ProductService;
import com.mall.vo.ProductDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "商品", description = "商品公开接口")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/page")
    @Operation(summary = "商品分页列表", description = "获取上架商品分页列表")
    public Result<PageResult<Product>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String sort) {
        Page<Product> page = productService.publicPage(current, size, keyword, categoryId, brandId,
                minPrice, maxPrice, minRating, inStock, sort);
        PageResult<Product> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @GetMapping("/suggestions")
    public Result<List<String>> suggestions(@RequestParam String keyword,
                                             @RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(productService.suggestions(keyword, limit));
    }

    @GetMapping("/popular-searches")
    public Result<List<String>> popularSearches(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(productService.popularSearches(limit));
    }

    @GetMapping("/{id}/related")
    public Result<List<Product>> related(@PathVariable Long id,
                                          @RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(productService.related(id, limit));
    }

    @GetMapping("/recommendations")
    public Result<List<Product>> recommendations(@AuthenticationPrincipal Long userId,
                                                  @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(productService.recommendations(userId, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情", description = "获取商品详情，包含SKU列表和图片")
    public Result<ProductDetailVO> getDetail(@PathVariable Long id) {
        ProductDetailVO detail = productService.getDetail(id);
        return Result.success(detail);
    }
}
