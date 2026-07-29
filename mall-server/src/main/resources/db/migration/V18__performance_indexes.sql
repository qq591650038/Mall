CREATE INDEX idx_favorite_price_alert_id ON favorite (price_alert, id);
CREATE INDEX idx_favorite_stock_alert_id ON favorite (stock_alert, id);
CREATE INDEX idx_participant_expire ON marketing_participant (activity_id, group_status, status, group_no);
CREATE INDEX idx_order_status_create_time ON `order` (order_status, create_time);
CREATE INDEX idx_payment_status_time ON payment (payment_status, payment_time);
CREATE INDEX idx_review_product_status_parent_rating ON review (product_id, status, parent_id, rating);
CREATE INDEX idx_points_account_balance_user ON points_account (balance, user_id);
CREATE INDEX idx_order_paid_status_user_amount ON `order` (pay_status, order_status, user_id, pay_amount);
CREATE INDEX idx_product_status_sales ON product (status, sales, id);
