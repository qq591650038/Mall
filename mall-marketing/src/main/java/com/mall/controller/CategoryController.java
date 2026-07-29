package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.Category;
import com.mall.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "商品分类", description = "商品分类公开接口")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取全部分类", description = "获取所有分类列表")
    public Result<List<Category>> list() {
        List<Category> list = categoryService.listAll();
        return Result.success(list);
    }

    @GetMapping("/parent/{parentId}")
    @Operation(summary = "获取子分类", description = "根据父级ID获取子分类列表")
    public Result<List<Category>> listByParent(@PathVariable Long parentId) {
        List<Category> list = categoryService.listByParentId(parentId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情", description = "根据ID获取分类详情")
    public Result<Category> getById(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        return Result.success(category);
    }
}