package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.UserSpending;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserSpendingMapper extends BaseMapper<UserSpending> {
    int adjust(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
