package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.Notification;
import com.mall.mapper.NotificationMapper;
import com.mall.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public Page<Notification> page(Long userId, Integer current, Integer size, Boolean unreadOnly) {
        Page<Notification> page = new Page<>(current, size);
        QueryWrapper<Notification> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        if (Boolean.TRUE.equals(unreadOnly)) {
            query.eq("is_read", 0);
        }
        query.orderByDesc("create_time");
        return notificationMapper.selectPage(page, query);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !userId.equals(notification.getUserId())) {
            return;
        }
        if (notification.getIsRead() == null || notification.getIsRead() == 0) {
            notification.setIsRead(1);
            notification.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        Notification update = new Notification();
        update.setIsRead(1);
        update.setReadTime(LocalDateTime.now());
        notificationMapper.update(update, new QueryWrapper<Notification>()
                .eq("user_id", userId)
                .eq("is_read", 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notify(Long userId, String type, String title, String content, String businessType, Long businessId) {
        if (userId == null || title == null || title.isBlank()) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBusinessType(businessType);
        notification.setBusinessId(businessId);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }
}
