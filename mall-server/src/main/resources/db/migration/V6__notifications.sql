-- 用户站内消息，保存订单、退款、优惠券等业务提醒。
CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) DEFAULT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500) DEFAULT NULL,
    business_type VARCHAR(40) DEFAULT NULL,
    business_id BIGINT DEFAULT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_notification_user_read (user_id, is_read),
    KEY idx_notification_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户站内消息';
