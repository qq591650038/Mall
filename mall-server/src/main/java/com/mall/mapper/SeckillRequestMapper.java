package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.SeckillRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SeckillRequestMapper extends BaseMapper<SeckillRequest> {
    SeckillRequest selectForUpdate(@Param("requestId") String requestId);
    int markSucceeded(@Param("requestId") String requestId, @Param("orderId") Long orderId);
    int markFailed(@Param("requestId") String requestId, @Param("errorMessage") String errorMessage);
    int markCompensated(@Param("requestId") String requestId);
}
