package com.mall.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Refund;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefundMapper extends BaseMapper<Refund> {
    java.math.BigDecimal sumRefundedAmountByOrderId(Long orderId);

    java.util.List<Refund> selectAdminCursorPage(@org.apache.ibatis.annotations.Param("status") Integer status,
                                                 @org.apache.ibatis.annotations.Param("orderNo") String orderNo,
                                                 @org.apache.ibatis.annotations.Param("cursorTime") java.time.LocalDateTime cursorTime,
                                                 @org.apache.ibatis.annotations.Param("cursorId") Long cursorId,
                                                 @org.apache.ibatis.annotations.Param("limit") int limit);
}
