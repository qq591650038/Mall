package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.MemberLevel;
import com.mall.service.MemberLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 会员等级控制器 */
@RestController
@RequestMapping("/api")
@Tag(name = "会员等级", description = "会员等级管理接口")
public class MemberLevelController {

    private final MemberLevelService memberLevelService;

    public MemberLevelController(MemberLevelService memberLevelService) {
        this.memberLevelService = memberLevelService;
    }

    // ==================== 用户端接口 ====================

    @GetMapping("/member-levels/list")
    @Operation(summary = "获取所有启用的会员等级", description = "获取所有启用的会员等级列表")
    public Result<List<MemberLevel>> list() {
        return Result.success(memberLevelService.listAll());
    }

    // ==================== 管理端接口 ====================

    @GetMapping("/admin/member-levels/page")
    @Operation(summary = "分页获取会员等级", description = "管理端分页获取会员等级列表")
    public Result<PageResult<MemberLevel>> page(@RequestParam(defaultValue = "1") Integer current,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) Integer status) {
        Page<MemberLevel> page = memberLevelService.page(current, size, status);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), current, size));
    }

    @GetMapping("/admin/member-levels/{id}")
    @Operation(summary = "获取会员等级详情", description = "管理端获取会员等级详情")
    public Result<MemberLevel> getById(@PathVariable Long id) {
        return Result.success(memberLevelService.getById(id));
    }

    @PostMapping("/admin/member-levels")
    @Operation(summary = "创建会员等级", description = "管理端创建会员等级")
    public Result<MemberLevel> create(@RequestBody MemberLevel level) {
        return Result.success(memberLevelService.create(level));
    }

    @PutMapping("/admin/member-levels/{id}")
    @Operation(summary = "更新会员等级", description = "管理端更新会员等级")
    public Result<MemberLevel> update(@PathVariable Long id, @RequestBody MemberLevel level) {
        return Result.success(memberLevelService.update(id, level));
    }

    @DeleteMapping("/admin/member-levels/{id}")
    @Operation(summary = "删除会员等级", description = "管理端删除会员等级")
    public Result<Void> delete(@PathVariable Long id) {
        memberLevelService.delete(id);
        return Result.success("删除成功", null);
    }
}
