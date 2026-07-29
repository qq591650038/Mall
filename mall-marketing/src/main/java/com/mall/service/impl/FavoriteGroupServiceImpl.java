package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.result.ErrorCode;
import com.mall.entity.FavoriteGroup;
import com.mall.entity.Favorite;
import com.mall.exception.BusinessException;
import com.mall.mapper.FavoriteGroupMapper;
import com.mall.mapper.FavoriteMapper;
import com.mall.service.FavoriteGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收藏分组服务实现类
 */
@Slf4j
@Service
public class FavoriteGroupServiceImpl implements FavoriteGroupService {

    private final FavoriteGroupMapper favoriteGroupMapper;
    private final FavoriteMapper favoriteMapper;

    public FavoriteGroupServiceImpl(FavoriteGroupMapper favoriteGroupMapper,
                                     FavoriteMapper favoriteMapper) {
        this.favoriteGroupMapper = favoriteGroupMapper;
        this.favoriteMapper = favoriteMapper;
    }

    @Override
    public List<FavoriteGroup> listByUserId(Long userId) {
        return favoriteGroupMapper.selectList(
                new QueryWrapper<FavoriteGroup>()
                        .eq("user_id", userId)
                        .orderByAsc("sort")
        );
    }

    @Override
    @Transactional
    public FavoriteGroup create(Long userId, String name, Integer sort) {
        // 校验分组名称不为空
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分组名称不能为空");
        }
        // 检查分组名称是否重复
        Long count = favoriteGroupMapper.selectCount(
                new QueryWrapper<FavoriteGroup>()
                        .eq("user_id", userId)
                        .eq("name", name)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE, "分组名称已存在");
        }
        FavoriteGroup group = new FavoriteGroup();
        group.setUserId(userId);
        group.setName(name);
        group.setSort(sort != null ? sort : 0);
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        favoriteGroupMapper.insert(group);
        log.info("创建收藏分组: userId={}, name={}", userId, name);
        return group;
    }

    @Override
    @Transactional
    public FavoriteGroup update(Long id, String name, Integer sort) {
        FavoriteGroup group = favoriteGroupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分组不存在");
        }
        // 校验分组名称不为空
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分组名称不能为空");
        }
        group.setName(name);
        if (sort != null) {
            group.setSort(sort);
        }
        group.setUpdateTime(LocalDateTime.now());
        favoriteGroupMapper.updateById(group);
        log.info("更新收藏分组: id={}, name={}", id, name);
        return group;
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        FavoriteGroup group = favoriteGroupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分组不存在");
        }
        // 检查是否是分组所有者
        if (!group.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该分组");
        }
        // 将该分组下的收藏移动到未分组（groupId设为null）
        List<Favorite> favorites = favoriteMapper.selectList(
                new QueryWrapper<Favorite>().eq("group_id", id)
        );
        for (Favorite fav : favorites) {
            fav.setGroupId(null);
            fav.setUpdateTime(LocalDateTime.now());
            favoriteMapper.updateById(fav);
        }
        // 删除分组
        favoriteGroupMapper.deleteById(id);
        log.info("删除收藏分组: id={}, 迁移收藏数量={}", id, favorites.size());
    }

    @Override
    public boolean isOwner(Long id, Long userId) {
        FavoriteGroup group = favoriteGroupMapper.selectById(id);
        return group != null && group.getUserId().equals(userId);
    }
}