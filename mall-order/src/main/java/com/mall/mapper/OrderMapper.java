package com.mall.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    int updateStatus(Long id, Long userId, Integer fromStatus, Integer toStatus);
    int cancelPending(Long id);
    int markPaid(Long id);
}
