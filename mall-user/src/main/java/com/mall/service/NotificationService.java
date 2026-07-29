package com.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.Notification;

public interface NotificationService {
    Page<Notification> page(Long userId, Integer current, Integer size, Boolean unreadOnly);

    long countUnread(Long userId);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);

    void notify(Long userId, String type, String title, String content, String businessType, Long businessId);
}
