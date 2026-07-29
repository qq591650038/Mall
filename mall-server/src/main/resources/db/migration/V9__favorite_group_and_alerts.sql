-- =============================================
-- V9: 收藏分组与价格/库存提醒
-- 日期: 2026-07-29
-- =============================================

-- 创建收藏分组表（如果不存在）
CREATE TABLE IF NOT EXISTS `favorite_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分组名称',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏分组表';

-- 为收藏表添加分组ID字段（如果不存在）
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `favorite` ADD COLUMN `group_id` BIGINT DEFAULT NULL COMMENT ''分组ID'' AFTER `product_id`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND COLUMN_NAME = 'group_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为收藏表添加收藏时价格字段（如果不存在）
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `favorite` ADD COLUMN `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT ''收藏时的价格'' AFTER `group_id`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND COLUMN_NAME = 'original_price'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为收藏表添加降价提醒开关字段（如果不存在）
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `favorite` ADD COLUMN `price_alert` TINYINT DEFAULT 1 COMMENT ''降价提醒 0-关闭 1-开启'' AFTER `original_price`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND COLUMN_NAME = 'price_alert'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为收藏表添加到货提醒开关字段（如果不存在）
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `favorite` ADD COLUMN `stock_alert` TINYINT DEFAULT 0 COMMENT ''到货提醒 0-关闭 1-开启'' AFTER `price_alert`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND COLUMN_NAME = 'stock_alert'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为收藏表添加最后检查价格字段（如果不存在）
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `favorite` ADD COLUMN `last_price` DECIMAL(10,2) DEFAULT NULL COMMENT ''最后检查价格'' AFTER `stock_alert`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND COLUMN_NAME = 'last_price'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为收藏表添加最后检查库存字段（如果不存在）
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `favorite` ADD COLUMN `last_stock` INT DEFAULT NULL COMMENT ''最后检查库存'' AFTER `last_price`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND COLUMN_NAME = 'last_stock'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加分组索引（如果不存在）
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `favorite` ADD INDEX `idx_group_id` (`group_id`)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND INDEX_NAME = 'idx_group_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加用户分组联合索引（如果不存在）
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `favorite` ADD INDEX `idx_user_group` (`user_id`, `group_id`)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND INDEX_NAME = 'idx_user_group'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;