package com.mall.service;

import com.mall.entity.FavoriteGroup;
import java.util.List;

/**
 * 收藏分组服务接口
 */
public interface FavoriteGroupService {

    /** 获取用户的所有收藏分组 */
    List<FavoriteGroup> listByUserId(Long userId);

    /** 新增收藏分组 */
    FavoriteGroup create(Long userId, String name, Integer sort);

    /** 更新收藏分组 */
    FavoriteGroup update(Long id, String name, Integer sort);

    /** 删除收藏分组 */
    void delete(Long id, Long userId);

    /** 检查分组是否属于该用户 */
    boolean isOwner(Long id, Long userId);
}