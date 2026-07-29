-- 会员等级表
CREATE TABLE IF NOT EXISTS `member_level` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `level` INT NOT NULL COMMENT '等级数值',
    `min_points` INT NOT NULL DEFAULT 0 COMMENT '最少积分',
    `max_points` INT NOT NULL DEFAULT 0 COMMENT '最多积分（0表示无上限）',
    `points_rate` DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '积分倍率',
    `discount_rate` DECIMAL(3,2) DEFAULT 1.00 COMMENT '折扣率',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '等级图标',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '等级描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_level_status` (`level`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级表';

-- 初始化默认等级数据
INSERT INTO `member_level` (`name`, `level`, `min_points`, `max_points`, `points_rate`, `discount_rate`, `icon`, `description`, `status`, `sort`)
SELECT '普通会员', 1, 0, 999, 1.00, 1.00, '🥉', '注册即为普通会员，享受基础积分权益', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `member_level` WHERE `level` = 1);
INSERT INTO `member_level` (`name`, `level`, `min_points`, `max_points`, `points_rate`, `discount_rate`, `icon`, `description`, `status`, `sort`)
SELECT '白银会员', 2, 1000, 4999, 1.20, 0.98, '🥈', '满1000积分升级，积分1.2倍，享98折', 1, 2
WHERE NOT EXISTS (SELECT 1 FROM `member_level` WHERE `level` = 2);
INSERT INTO `member_level` (`name`, `level`, `min_points`, `max_points`, `points_rate`, `discount_rate`, `icon`, `description`, `status`, `sort`)
SELECT '黄金会员', 3, 5000, 19999, 1.50, 0.95, '🥇', '满5000积分升级，积分1.5倍，享95折', 1, 3
WHERE NOT EXISTS (SELECT 1 FROM `member_level` WHERE `level` = 3);
INSERT INTO `member_level` (`name`, `level`, `min_points`, `max_points`, `points_rate`, `discount_rate`, `icon`, `description`, `status`, `sort`)
SELECT '钻石会员', 4, 20000, 0, 2.00, 0.90, '💎', '满20000积分升级，积分2倍，享9折优惠', 1, 4
WHERE NOT EXISTS (SELECT 1 FROM `member_level` WHERE `level` = 4);

-- 用户表增加会员等级字段
SET @db_name = DATABASE();

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `user` ADD COLUMN `member_level_id` BIGINT DEFAULT 1 COMMENT ''会员等级ID''',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'user' AND column_name = 'member_level_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `points_account` ADD COLUMN `member_level_id` BIGINT DEFAULT 1 COMMENT ''会员等级ID''',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'points_account' AND column_name = 'member_level_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
