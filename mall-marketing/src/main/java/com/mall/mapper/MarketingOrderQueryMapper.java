package com.mall.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MarketingOrderQueryMapper {
    @Select("<script>SELECT id, order_no AS orderNo, order_status AS orderStatus, pay_status AS payStatus FROM `order` WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<OrderStatusRow> findStatuses(List<Long> ids);

    @Select("<script>SELECT id, username FROM user WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<UserRow> findUsers(List<Long> ids);

    class OrderStatusRow {
        public Long id;
        public String orderNo;
        public Integer orderStatus;
        public Integer payStatus;
    }

    class UserRow {
        public Long id;
        public String username;
    }
}
