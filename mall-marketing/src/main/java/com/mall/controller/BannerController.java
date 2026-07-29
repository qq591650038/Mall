package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.Banner;
import com.mall.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banner")
@Tag(name = "轮播图", description = "首页轮播图接口")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取启用的轮播图", description = "获取当前启用的轮播图列表")
    public Result<List<Banner>> listActive() {
        List<Banner> list = bannerService.listActive();
        return Result.success(list);
    }
}