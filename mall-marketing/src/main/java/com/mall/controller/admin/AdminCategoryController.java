package com.mall.controller.admin;

import com.mall.common.result.Result;
import com.mall.entity.Category;
import com.mall.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@Tag(name = "分类管理", description = "后台分类管理接口")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    @Operation(summary = "全部分类列表", description = "获取所有分类列表")
    public Result<List<Category>> list() {
        List<Category> list = categoryService.listAll();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "分类详情", description = "根据ID获取分类详情")
    public Result<Category> getById(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        return Result.success(category);
    }

    @PostMapping
    @Operation(summary = "新增分类", description = "新增商品分类")
    public Result<Void> save(@RequestBody Category category) {
        categoryService.save(category);
        return Result.success("新增成功", null);
    }

    @PutMapping
    @Operation(summary = "更新分类", description = "更新分类信息")
    public Result<Void> update(@RequestBody Category category) {
        categoryService.update(category);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteById(id);
        return Result.success("删除成功", null);
    }
}