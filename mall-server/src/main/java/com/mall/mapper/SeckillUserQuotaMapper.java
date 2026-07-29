package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.SeckillUserQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SeckillUserQuotaMapper extends BaseMapper<SeckillUserQuota> {
    int ensureRow(@Param("itemId") Long itemId, @Param("userId") Long userId);

    int reserve(@Param("itemId") Long itemId, @Param("userId") Long userId,
                @Param("quantity") Integer quantity, @Param("limit") Integer limit);

    int release(@Param("itemId") Long itemId, @Param("userId") Long userId,
                @Param("quantity") Integer quantity);
}
