package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.Address;
import com.mall.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "收货地址", description = "收货地址管理接口")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取地址列表", description = "获取当前用户的收货地址列表")
    public Result<List<Address>> list(@AuthenticationPrincipal Long userId) {
        List<Address> list = addressService.listByUserId(userId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取地址详情", description = "获取地址详情")
    public Result<Address> getById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        Address address = addressService.getById(id, userId);
        return Result.success(address);
    }

    @PostMapping
    @Operation(summary = "新增地址", description = "新增收货地址")
    public Result<Void> save(@AuthenticationPrincipal Long userId, @RequestBody Address address) {
        address.setUserId(userId);
        addressService.save(address);
        return Result.success("新增成功", null);
    }

    @PutMapping
    @Operation(summary = "更新地址", description = "更新收货地址")
    public Result<Void> update(@AuthenticationPrincipal Long userId, @RequestBody Address address) {
        address.setUserId(userId);
        addressService.update(address);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址", description = "删除收货地址")
    public Result<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        addressService.delete(id, userId);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认地址", description = "设置默认收货地址")
    public Result<Void> setDefault(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        addressService.setDefault(id, userId);
        return Result.success("设置成功", null);
    }
}