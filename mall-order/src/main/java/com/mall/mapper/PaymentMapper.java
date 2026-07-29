package com.mall.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
    int markPaid(String paymentNo);

    @Select("SELECT p.* FROM payment p WHERE p.payment_status = 1 "
            + "AND NOT EXISTS (SELECT 1 FROM points_ledger l WHERE l.event_type = 'PAYMENT_EARN' "
            + "AND l.business_id = p.order_id) ORDER BY p.payment_time ASC LIMIT #{limit}")
    List<Payment> findPaidWithoutPoints(@Param("limit") int limit);
}
