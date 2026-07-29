package com.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.dto.LoginDTO;
import com.mall.dto.RegisterDTO;
import com.mall.dto.UpdateUserDTO;
import com.mall.entity.User;
import com.mall.vo.LoginVO;
import com.mall.vo.UserVO;

public interface UserService {
    void register(RegisterDTO registerDTO);

    LoginVO login(LoginDTO loginDTO);

    void logout(Long userId);

    UserVO getUserInfo(Long userId);

    void updateUserInfo(Long userId, UpdateUserDTO updateUserDTO);

    Page<UserVO> pageForAdmin(Integer current, Integer size, String keyword, Integer status);

    com.mall.common.result.CursorPageResult<UserVO> cursorPageForAdmin(Integer size, String keyword, Integer status, String cursor);

    UserVO getUserInfoForAdmin(Long userId);

    void updateStatusForAdmin(Long userId, Integer status);

    User findById(Long id);

    User findByPhone(String phone);

    User findByEmail(String email);

    User findByUsername(String username);
}
