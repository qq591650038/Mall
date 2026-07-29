package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.Brand;
import com.mall.service.BrandService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/brands")
public class BrandController {
    private final BrandService brandService;
    public BrandController(BrandService brandService) { this.brandService = brandService; }
    @GetMapping("/list")
    public Result<List<Brand>> list() { return Result.success(brandService.listEnabled()); }
}
