package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.Notification;
import com.mall.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/page")
    public Result<PageResult<Notification>> page(@AuthenticationPrincipal Long userId,
                                                  @RequestParam(defaultValue = "1") Integer current,
                                                  @RequestParam(defaultValue = "20") Integer size,
                                                  @RequestParam(required = false) Boolean unreadOnly) {
        Page<Notification> page = notificationService.page(userId, current, size, unreadOnly);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), current, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@AuthenticationPrincipal Long userId) {
        return Result.success(notificationService.countUnread(userId));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return Result.success("已读", null);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllRead(userId);
        return Result.success("全部已读", null);
    }
}
