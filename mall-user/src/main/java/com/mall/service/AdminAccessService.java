package com.mall.service;
import com.mall.entity.Permission;
import com.mall.entity.Role;
import java.util.List;
public interface AdminAccessService {
    List<Role> roles();
    List<Permission> permissions();
    void save(Role role);
    void update(Long id, Role role);
    void delete(Long id);
    void grant(Long roleId, Long permissionId);
    void revoke(Long roleId, Long permissionId);
}
