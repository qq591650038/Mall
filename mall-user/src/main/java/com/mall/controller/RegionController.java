package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.Region;
import com.mall.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@Tag(name = "地区数据", description = "省市区数据接口")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping("/provinces")
    @Operation(summary = "获取省份列表", description = "获取所有省份")
    public Result<List<Region>> getProvinces() {
        return Result.success(regionService.getProvinces());
    }

    @GetMapping("/cities/{provinceId}")
    @Operation(summary = "获取城市列表", description = "根据省份ID获取城市列表")
    public Result<List<Region>> getCities(@PathVariable Long provinceId) {
        return Result.success(regionService.getCitiesByProvince(provinceId));
    }

    @GetMapping("/districts/{cityId}")
    @Operation(summary = "获取区县列表", description = "根据城市ID获取区县列表")
    public Result<List<Region>> getDistricts(@PathVariable Long cityId) {
        return Result.success(regionService.getDistrictsByCity(cityId));
    }
}
