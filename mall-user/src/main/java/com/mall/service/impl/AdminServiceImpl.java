package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.result.ErrorCode;
import com.mall.dto.admin.AdminLoginDTO;
import com.mall.entity.Admin;
import com.mall.entity.AdminRole;
import com.mall.entity.Permission;
import com.mall.entity.Role;
import com.mall.entity.RolePermission;
import com.mall.exception.BusinessException;
import com.mall.mapper.*;
import com.mall.security.JwtTokenProvider;
import com.mall.service.AdminService;
import com.mall.utils.PasswordUtil;
import com.mall.utils.RedisUtil;
import com.mall.vo.admin.AdminLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtil redisUtil;
    private final PasswordUtil passwordUtil;

    public AdminServiceImpl(AdminMapper adminMapper,
                            AdminRoleMapper adminRoleMapper,
                            RoleMapper roleMapper,
                            RolePermissionMapper rolePermissionMapper,
                            PermissionMapper permissionMapper,
                            JwtTokenProvider jwtTokenProvider,
                            RedisUtil redisUtil,
                            PasswordUtil passwordUtil) {
        this.adminMapper = adminMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisUtil = redisUtil;
        this.passwordUtil = passwordUtil;
    }

    @Override
    public AdminLoginVO login(AdminLoginDTO loginDTO) {
        Admin admin = adminMapper.selectOne(
                new QueryWrapper<Admin>().eq("username", loginDTO.getUsername())
        );

        if (admin == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "管理员不存在");
        }

        if (admin.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "账号已被禁用");
        }

        if (admin.getPassword() == null || admin.getPassword().isEmpty()) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "请先初始化管理员密码");
        }

        if (!passwordUtil.matches(loginDTO.getPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        String token = jwtTokenProvider.generateAdminToken(admin.getId(), "ADMIN");

        String loginKey = "login:admin:" + admin.getId();
        redisUtil.set(loginKey, token, 24, TimeUnit.HOURS);

        admin.setLastLoginTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        adminMapper.updateById(admin);

        List<String> roles = getAdminRoles(admin.getId());
        List<String> permissions = getAdminPermissions(admin.getId());

        log.info("管理员登录成功: adminId={}", admin.getId());

        return buildAdminLoginVO(admin, token, roles, permissions);
    }

    @Override
    public void logout(Long adminId) {
        String loginKey = "login:admin:" + adminId;
        redisUtil.delete(loginKey);
        log.info("管理员退出登录: adminId={}", adminId);
    }

    @Override
    public Admin findById(Long id) {
        return adminMapper.selectById(id);
    }

    @Override
    public Admin findByUsername(String username) {
        return adminMapper.selectOne(
                new QueryWrapper<Admin>().eq("username", username)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeDefaultAdmin() {
        Admin admin = findByUsername("admin");
        if (admin == null) {
            admin = new Admin();
            admin.setUsername("admin");
            admin.setRealName("超级管理员");
            admin.setStatus(1);
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());
            admin.setDeleted(0);
            admin.setPassword(passwordUtil.encode("admin123"));
            adminMapper.insert(admin);
            log.info("默认管理员初始化成功: admin/admin123");

            AdminRole adminRole = new AdminRole();
            adminRole.setAdminId(admin.getId());
            adminRole.setRoleId(1L);
            adminRole.setCreateTime(LocalDateTime.now());
            adminRoleMapper.insert(adminRole);
        } else if (admin.getPassword() == null || admin.getPassword().isEmpty()) {
            admin.setPassword(passwordUtil.encode("admin123"));
            admin.setUpdateTime(LocalDateTime.now());
            adminMapper.updateById(admin);
            log.info("默认管理员密码重置成功: admin/admin123");

            Long roleCount = adminRoleMapper.selectCount(
                    new QueryWrapper<AdminRole>().eq("admin_id", admin.getId())
            );
            if (roleCount == 0) {
                AdminRole adminRole = new AdminRole();
                adminRole.setAdminId(admin.getId());
                adminRole.setRoleId(1L);
                adminRole.setCreateTime(LocalDateTime.now());
                adminRoleMapper.insert(adminRole);
            }
        }
    }

    private List<String> getAdminRoles(Long adminId) {
        List<AdminRole> adminRoles = adminRoleMapper.selectList(
                new QueryWrapper<AdminRole>().eq("admin_id", adminId)
        );
        if (adminRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = adminRoles.stream().map(AdminRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().map(Role::getCode).collect(Collectors.toList());
    }

    private List<String> getAdminPermissions(Long adminId) {
        List<AdminRole> adminRoles = adminRoleMapper.selectList(
                new QueryWrapper<AdminRole>().eq("admin_id", adminId)
        );
        if (adminRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = adminRoles.stream().map(AdminRole::getRoleId).collect(Collectors.toList());
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new QueryWrapper<RolePermission>().in("role_id", roleIds)
        );
        if (rolePermissions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> permissionIds = rolePermissions.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
        List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
        return permissions.stream().map(Permission::getCode).collect(Collectors.toList());
    }

    private AdminLoginVO buildAdminLoginVO(Admin admin, String token, List<String> roles, List<String> permissions) {
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setExpiresIn(86400L);

        AdminLoginVO.AdminInfoVO adminInfo = new AdminLoginVO.AdminInfoVO();
        adminInfo.setId(admin.getId());
        adminInfo.setUsername(admin.getUsername());
        adminInfo.setRealName(admin.getRealName());
        adminInfo.setAvatar(admin.getAvatar());
        adminInfo.setEmail(admin.getEmail());
        adminInfo.setPhone(admin.getPhone());
        adminInfo.setRoles(roles);
        adminInfo.setPermissions(permissions);
        vo.setAdminInfo(adminInfo);

        return vo;
    }
}