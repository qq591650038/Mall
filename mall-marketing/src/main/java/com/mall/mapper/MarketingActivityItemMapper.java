package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.MarketingActivityItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销活动商品明细 Mapper
 */
@Mapper
public interface MarketingActivityItemMapper extends BaseMapper<MarketingActivityItem> {
    int deductForSeckill(@org.apache.ibatis.annotations.Param("itemId") Long itemId,
                          @org.apache.ibatis.annotations.Param("quantity") Integer quantity);
}
