package com.mall.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.CursorPageResult;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.service.UserService;
import com.mall.vo.UserVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "20") Integer size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status) {
        Page<UserVO> page = userService.pageForAdmin(current, size, keyword, status);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), current, size));
    }

    @GetMapping("/cursor")
    public Result<CursorPageResult<UserVO>> cursorPage(@RequestParam(defaultValue = "20") Integer size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) String cursor) {
        return Result.success(userService.cursorPageForAdmin(size, keyword, status, cursor));
    }

    @GetMapping("/{id}")
    public Result<UserVO> get(@PathVariable Long id) {
        return Result.success(userService.getUserInfoForAdmin(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        userService.updateStatusForAdmin(id, request.status());
        return Result.success("用户状态已更新", null);
    }

    public record StatusRequest(Integer status) { }
}
