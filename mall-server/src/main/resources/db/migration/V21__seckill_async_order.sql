CREATE TABLE seckill_request
(
    request_id       VARCHAR(64) NOT NULL,
    activity_id      BIGINT      NOT NULL,
    activity_item_id BIGINT      NOT NULL,
    user_id          BIGINT      NOT NULL,
    quantity         INT         NOT NULL,
    address_id       BIGINT      NOT NULL,
    status           TINYINT     NOT NULL DEFAULT 0 COMMENT '0 pending, 1 success, 2 failed',
    order_id         BIGINT               DEFAULT NULL,
    error_message    VARCHAR(500)         DEFAULT NULL,
    compensated      TINYINT     NOT NULL DEFAULT 0,
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (request_id),
    KEY              idx_seckill_request_user_time (user_id, create_time),
    KEY              idx_seckill_request_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Async seckill order requests';

ALTER TABLE marketing_participant
    ADD COLUMN request_id VARCHAR(64) DEFAULT NULL AFTER id,
    ADD UNIQUE KEY uk_marketing_participant_request (request_id);
