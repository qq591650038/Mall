package com.mall.service;

import com.mall.dto.admin.AdminLoginDTO;
import com.mall.entity.Admin;
import com.mall.vo.admin.AdminLoginVO;

public interface AdminService {

    AdminLoginVO login(AdminLoginDTO loginDTO);

    void logout(Long adminId);

    Admin findById(Long id);

    Admin findByUsername(String username);

    void initializeDefaultAdmin();
}