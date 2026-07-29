CREATE TABLE seckill_user_quota
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    activity_item_id  BIGINT       NOT NULL,
    user_id           BIGINT       NOT NULL,
    reserved_quantity INT          NOT NULL DEFAULT 0,
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seckill_user_quota (activity_item_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Seckill user reserved purchase quota';
