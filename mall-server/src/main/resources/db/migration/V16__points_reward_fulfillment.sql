ALTER TABLE points_product
    ADD COLUMN reward_ref_id BIGINT DEFAULT NULL AFTER reward_type,
    ADD COLUMN reward_sku_id BIGINT DEFAULT NULL AFTER reward_ref_id,
    ADD KEY idx_points_product_reward (reward_type, reward_ref_id);

ALTER TABLE points_redemption
    MODIFY COLUMN redemption_code VARCHAR(64) DEFAULT NULL,
    ADD COLUMN reward_type VARCHAR(30) DEFAULT NULL AFTER redemption_code,
    ADD COLUMN reward_ref_id BIGINT DEFAULT NULL AFTER reward_type,
    ADD COLUMN reward_sku_id BIGINT DEFAULT NULL AFTER reward_ref_id,
    ADD COLUMN order_id BIGINT DEFAULT NULL AFTER reward_sku_id,
    ADD COLUMN user_coupon_id BIGINT DEFAULT NULL AFTER order_id,
    ADD COLUMN fulfillment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' AFTER user_coupon_id,
    ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time,
    ADD KEY idx_points_redemption_order (order_id),
    ADD KEY idx_points_redemption_user_coupon (user_coupon_id);

ALTER TABLE `order`
    ADD COLUMN order_source VARCHAR(30) NOT NULL DEFAULT 'NORMAL' AFTER coupon_id,
    ADD COLUMN order_type VARCHAR(30) NOT NULL DEFAULT 'PHYSICAL' AFTER order_source,
    ADD KEY idx_order_source_type (order_source, order_type);
