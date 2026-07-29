package com.mall.controller.admin;

import com.mall.common.result.Result;
import com.mall.entity.Banner;
import com.mall.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@Tag(name = "轮播图管理", description = "后台轮播图管理接口")
public class AdminBannerController {

    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping("/list")
    @Operation(summary = "轮播图列表", description = "获取所有轮播图列表")
    public Result<List<Banner>> list() {
        List<Banner> list = bannerService.listAll();
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "新增轮播图", description = "新增轮播图")
    public Result<Void> save(@RequestBody Banner banner) {
        bannerService.save(banner);
        return Result.success("新增成功", null);
    }

    @PutMapping
    @Operation(summary = "更新轮播图", description = "更新轮播图信息")
    public Result<Void> update(@RequestBody Banner banner) {
        bannerService.update(banner);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除轮播图", description = "删除轮播图")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.deleteById(id);
        return Result.success("删除成功", null);
    }
}