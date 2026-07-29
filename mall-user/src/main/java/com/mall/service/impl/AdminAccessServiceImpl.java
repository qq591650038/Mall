package com.mall.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.entity.Permission;
import com.mall.entity.Role;
import com.mall.entity.RolePermission;
import com.mall.mapper.PermissionMapper;
import com.mall.mapper.RoleMapper;
import com.mall.mapper.RolePermissionMapper;
import com.mall.service.AdminAccessService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class AdminAccessServiceImpl implements AdminAccessService {
    private final RoleMapper roles; private final PermissionMapper permissions; private final RolePermissionMapper rolePermissions;
    public AdminAccessServiceImpl(RoleMapper roles, PermissionMapper permissions, RolePermissionMapper rolePermissions) { this.roles=roles; this.permissions=permissions; this.rolePermissions=rolePermissions; }
    public List<Role> roles(){ return roles.selectList(null); }
    public List<Permission> permissions(){ return permissions.selectList(null); }
    public void save(Role role){ role.setDeleted(0); role.setCreateTime(LocalDateTime.now()); role.setUpdateTime(LocalDateTime.now()); roles.insert(role); }
    public void update(Long id, Role role){ role.setId(id); role.setUpdateTime(LocalDateTime.now()); roles.updateById(role); }
    public void delete(Long id){ roles.deleteById(id); }
    public void grant(Long roleId, Long permissionId){
        long count=rolePermissions.selectCount(new QueryWrapper<RolePermission>().eq("role_id",roleId).eq("permission_id",permissionId));
        if(count==0){ RolePermission item=new RolePermission(); item.setRoleId(roleId); item.setPermissionId(permissionId); item.setCreateTime(LocalDateTime.now()); rolePermissions.insert(item); }
    }
    public void revoke(Long roleId, Long permissionId){ rolePermissions.delete(new QueryWrapper<RolePermission>().eq("role_id",roleId).eq("permission_id",permissionId)); }
}
