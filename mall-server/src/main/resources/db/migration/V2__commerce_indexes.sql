-- Incremental production-safe indexes; V1 is represented by the existing baseline schema.
CREATE INDEX idx_refund_status_time ON refund (status, create_time);
CREATE INDEX idx_payment_order_status ON payment (order_id, payment_status);
ALTER TABLE inventory_log ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
