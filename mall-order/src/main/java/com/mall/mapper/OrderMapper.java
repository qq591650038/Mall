package com.mall.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    int updateStatus(Long id, Long userId, Integer fromStatus, Integer toStatus);
    int cancelPending(Long id);
    int markPaid(Long id);

    java.util.List<Order> selectAdminCursorPage(@org.apache.ibatis.annotations.Param("status") Integer status,
                                                @org.apache.ibatis.annotations.Param("orderNo") String orderNo,
                                                @org.apache.ibatis.annotations.Param("userId") Long userId,
                                                @org.apache.ibatis.annotations.Param("cursorTime") java.time.LocalDateTime cursorTime,
                                                @org.apache.ibatis.annotations.Param("cursorId") Long cursorId,
                                                @org.apache.ibatis.annotations.Param("limit") int limit);
}
