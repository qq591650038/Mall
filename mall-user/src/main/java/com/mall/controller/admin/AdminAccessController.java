package com.mall.controller.admin;

import com.mall.common.result.Result;
import com.mall.entity.Permission;
import com.mall.entity.Role;
import com.mall.service.AdminAccessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/access")
public class AdminAccessController {
    private final AdminAccessService service;

    public AdminAccessController(AdminAccessService service) {
        this.service = service;
    }

    @GetMapping("/roles")
    public Result<List<Role>> roles() {
        return Result.success(service.roles());
    }

    @GetMapping("/permissions")
    public Result<List<Permission>> permissions() {
        return Result.success(service.permissions());
    }

    @PostMapping("/roles")
    public Result<Void> saveRole(@RequestBody Role role) {
        service.save(role);
        return Result.success("创建成功", null);
    }

    @PutMapping("/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Role role) {
        service.update(id, role);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        service.delete(id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    public Result<Void> grant(@PathVariable Long roleId, @PathVariable Long permissionId) {
        service.grant(roleId, permissionId);
        return Result.success("授权成功", null);
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public Result<Void> revoke(@PathVariable Long roleId, @PathVariable Long permissionId) {
        service.revoke(roleId, permissionId);
        return Result.success("撤销成功", null);
    }
}
