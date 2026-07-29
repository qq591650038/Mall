package com.mall.controller.admin;

import com.mall.common.result.Result;
import com.mall.entity.Admin;
import com.mall.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/profile")
@Tag(name = "管理员信息", description = "管理员个人信息相关接口")
public class AdminProfileController {

    private final AdminService adminService;

    public AdminProfileController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/info")
    @Operation(summary = "获取管理员信息", description = "获取当前登录管理员的详细信息")
    public Result<Admin> getAdminInfo(@AuthenticationPrincipal Long adminId) {
        Admin admin = adminService.findById(adminId);
        if (admin != null) {
            admin.setPassword(null);
            admin.setSalt(null);
        }
        return Result.success(admin);
    }
}