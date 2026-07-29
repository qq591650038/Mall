package com.mall.controller.user;

import com.mall.common.result.Result;
import com.mall.service.UserService;
import com.mall.dto.UpdateUserDTO;
import com.mall.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户中心", description = "用户个人信息相关接口")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    public Result<UserVO> getUserInfo(@AuthenticationPrincipal Long userId) {
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    @PutMapping("/info")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateUserInfo(@AuthenticationPrincipal Long userId,
                                       @Valid @RequestBody UpdateUserDTO updateUserDTO) {
        userService.updateUserInfo(userId, updateUserDTO);
        return Result.success("更新成功", null);
    }
}
