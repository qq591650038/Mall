package com.mall.controller.admin;

import com.mall.common.result.Result;
import com.mall.service.DataInitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/data")
@Tag(name = "数据初始化", description = "模拟数据初始化接口")
public class DataInitController {

    private final DataInitService dataInitService;

    public DataInitController(DataInitService dataInitService) {
        this.dataInitService = dataInitService;
    }

    @RequestMapping("/init")
    @Operation(summary = "初始化模拟数据", description = "插入测试用的用户、商品、订单等数据")
    public Result<String> initMockData() {
        dataInitService.initMockData();
        return Result.success("数据初始化成功");
    }
}