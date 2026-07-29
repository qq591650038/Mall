package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.StockSubscription;
import com.mall.vo.StockSubscriptionVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockSubscriptionMapper extends BaseMapper<StockSubscription> {
    List<StockSubscriptionVO> selectActiveSubscriptionsByUserId(Long userId);
}
