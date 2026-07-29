CREATE TABLE shipping_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    delivery_method VARCHAR(30) NOT NULL,
    regions VARCHAR(1000) DEFAULT NULL,
    base_freight DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    free_amount DECIMAL(10,2) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id), KEY idx_shipping_template_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shipping templates';

ALTER TABLE `order`
    ADD COLUMN shipping_template_id BIGINT DEFAULT NULL AFTER address_id,
    ADD COLUMN delivery_method VARCHAR(30) DEFAULT NULL AFTER shipping_template_id;

CREATE TABLE customer_service_ticket (
    id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, order_id BIGINT DEFAULT NULL,
    refund_id BIGINT DEFAULT NULL, subject VARCHAR(200) NOT NULL, category VARCHAR(30) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0, priority TINYINT NOT NULL DEFAULT 0, handled_by BIGINT DEFAULT NULL,
    close_time DATETIME DEFAULT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id), KEY idx_ticket_user_time (user_id, create_time), KEY idx_ticket_status_time (status, create_time),
    KEY idx_ticket_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Customer service tickets';

CREATE TABLE customer_service_message (
    id BIGINT NOT NULL AUTO_INCREMENT, ticket_id BIGINT NOT NULL, sender_id BIGINT NOT NULL,
    sender_role VARCHAR(20) NOT NULL, content VARCHAR(2000) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id), KEY idx_ticket_message (ticket_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Customer service messages';

CREATE TABLE stock_subscription (
    id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
    sku_id BIGINT DEFAULT NULL, status TINYINT NOT NULL DEFAULT 0, notified_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id), UNIQUE KEY uk_stock_subscription (user_id, product_id, sku_id),
    KEY idx_stock_subscription_lookup (product_id, sku_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Stock arrival subscriptions';

CREATE TABLE user_risk_control (
    id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, risk_type VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NOT NULL, status TINYINT NOT NULL DEFAULT 1, created_by BIGINT DEFAULT NULL,
    expire_time DATETIME DEFAULT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id), KEY idx_risk_user_active (user_id, status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User risk controls';
