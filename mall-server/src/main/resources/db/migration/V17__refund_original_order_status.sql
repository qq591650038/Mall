ALTER TABLE `refund`
    ADD COLUMN original_order_status TINYINT DEFAULT NULL COMMENT '申请售后前的订单状态' AFTER type;
