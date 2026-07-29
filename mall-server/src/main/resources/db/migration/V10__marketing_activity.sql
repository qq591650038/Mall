-- =============================================
-- V10: 营销活动表
-- 日期: 2026-07-29
-- =============================================

-- 创建营销活动主表（如果不存在）
CREATE TABLE IF NOT EXISTS `marketing_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '活动名称',
    `type` VARCHAR(30) NOT NULL COMMENT '活动类型: LIMIT_TIME_DISCOUNT-限时折扣, FULL_REDUCTION-满减, SECKILL-秒杀, GROUP_BUY-拼团',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '活动描述',
    `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
    `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-未开始, 1-进行中, 2-已结束, 3-已取消',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`),
    KEY `idx_start_end_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销活动主表';

-- 创建营销活动商品明细表（如果不存在）
CREATE TABLE IF NOT EXISTS `marketing_activity_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_id` BIGINT DEFAULT NULL COMMENT 'SKU ID（可选，为空表示所有SKU）',
    `activity_price` DECIMAL(10,2) NOT NULL COMMENT '活动价格',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '活动库存',
    `sold_count` INT NOT NULL DEFAULT 0 COMMENT '已售数量',
    `limit_per_user` INT DEFAULT 1 COMMENT '每人限购数量',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_activity_product` (`activity_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销活动商品明细表';

-- 创建营销活动参与者表（如果不存在）
CREATE TABLE IF NOT EXISTS `marketing_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `activity_item_id` BIGINT NOT NULL COMMENT '活动商品明细ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '订单ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    `group_no` VARCHAR(50) DEFAULT NULL COMMENT '拼团编号（拼团活动使用）',
    `group_status` TINYINT DEFAULT NULL COMMENT '拼团状态: 1-拼团中, 2-拼团成功, 3-拼团失败',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父参与者ID（拼团团长为null，团员为团长ID）',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付, 2-已取消',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_group_no` (`group_no`),
    KEY `idx_activity_user` (`activity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销活动参与者表';
