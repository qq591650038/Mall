-- 将优惠券生命周期事件纳入统一审计日志表。
-- 使用 information_schema 判断结构，保证旧库升级和新库初始化都可以重复执行。
SET @db_name = DATABASE();

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE operation_log ADD COLUMN event_type VARCHAR(30) DEFAULT NULL',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND column_name = 'event_type');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE operation_log ADD COLUMN user_coupon_id BIGINT DEFAULT NULL',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND column_name = 'user_coupon_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE operation_log ADD COLUMN user_id BIGINT DEFAULT NULL',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND column_name = 'user_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE operation_log ADD COLUMN coupon_id BIGINT DEFAULT NULL',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND column_name = 'coupon_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE operation_log ADD COLUMN order_id BIGINT DEFAULT NULL',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND column_name = 'order_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE operation_log ADD COLUMN remark VARCHAR(255) DEFAULT NULL',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND column_name = 'remark');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'CREATE INDEX idx_operation_event_type ON operation_log (event_type)',
    'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND index_name = 'idx_operation_event_type');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'CREATE INDEX idx_operation_user_coupon ON operation_log (user_coupon_id)',
    'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND index_name = 'idx_operation_user_coupon');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'CREATE INDEX idx_operation_user_id ON operation_log (user_id)',
    'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND index_name = 'idx_operation_user_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'CREATE INDEX idx_operation_order_id ON operation_log (order_id)',
    'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'operation_log' AND index_name = 'idx_operation_order_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
