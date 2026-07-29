-- =============================================
-- 商城系统数据库表结构
-- 数据库: MySQL 8.0+
-- =============================================

CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mall;

CREATE TABLE IF NOT EXISTS `inventory_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sku_id` BIGINT NOT NULL, `product_id` BIGINT NOT NULL, `order_id` BIGINT DEFAULT NULL,
    `quantity` INT NOT NULL, `operation` VARCHAR(30) NOT NULL, `status` TINYINT NOT NULL DEFAULT 1,
    `error_message` VARCHAR(500) DEFAULT NULL, `retry_count` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_inventory_order` (`order_id`), KEY `idx_inventory_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存操作流水';

-- =============================================
-- 用户表
-- =============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `password` VARCHAR(100) NOT NULL COMMENT '加密后的密码',
    `salt` VARCHAR(50) NOT NULL COMMENT '密码盐值',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `gender` TINYINT DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 管理员表
-- =============================================
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '管理员用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '加密后的密码',
    `salt` VARCHAR(50) NOT NULL COMMENT '密码盐值',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- =============================================
-- 角色表
-- =============================================
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- =============================================
-- 权限表
-- =============================================
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父级ID 0表示顶级',
    `name` VARCHAR(50) NOT NULL COMMENT '权限名称',
    `code` VARCHAR(100) NOT NULL COMMENT '权限编码',
    `type` TINYINT NOT NULL COMMENT '类型 1-菜单 2-按钮 3-接口',
    `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径/接口路径',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- =============================================
-- 管理员角色关联表
-- =============================================
DROP TABLE IF EXISTS `admin_role`;
CREATE TABLE `admin_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `admin_id` BIGINT NOT NULL COMMENT '管理员ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_role` (`admin_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员角色关联表';

-- =============================================
-- 角色权限关联表
-- =============================================
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- =============================================
-- 商品分类表
-- =============================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父级ID 0表示顶级分类',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(200) DEFAULT NULL COMMENT '分类图标',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `level` TINYINT DEFAULT 1 COMMENT '层级 1-一级 2-二级 3-三级',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- =============================================
-- 品牌表
-- =============================================
DROP TABLE IF EXISTS `brand`;
CREATE TABLE `brand` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '品牌名称',
    `logo` VARCHAR(500) DEFAULT NULL COMMENT '品牌Logo',
    `description` TEXT DEFAULT NULL COMMENT '品牌描述',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌表';

-- =============================================
-- 商品表
-- =============================================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `brand_id` BIGINT DEFAULT NULL COMMENT '品牌ID',
    `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `subtitle` VARCHAR(200) DEFAULT NULL COMMENT '副标题',
    `main_image` VARCHAR(500) DEFAULT NULL COMMENT '主图',
    `price` DECIMAL(10,2) NOT NULL COMMENT '最低价格',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
    `sales` INT NOT NULL DEFAULT 0 COMMENT '销量',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0-下架 1-上架',
    `is_recommend` TINYINT DEFAULT 0 COMMENT '是否推荐 0-否 1-是',
    `description` TEXT DEFAULT NULL COMMENT '商品描述(富文本)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_brand_id` (`brand_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_recommend` (`is_recommend`),
    FULLTEXT KEY `ft_product_search` (`name`, `subtitle`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- =============================================
-- 商品SKU表
-- =============================================
DROP TABLE IF EXISTS `product_sku`;
CREATE TABLE `product_sku` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_code` VARCHAR(100) NOT NULL COMMENT 'SKU编码',
    `spec_info` VARCHAR(500) NOT NULL COMMENT '规格信息(JSON)',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
    `image` VARCHAR(500) DEFAULT NULL COMMENT 'SKU图片',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_code` (`sku_code`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU表';

-- =============================================
-- 商品图片表
-- =============================================
DROP TABLE IF EXISTS `product_image`;
CREATE TABLE `product_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `url` VARCHAR(500) NOT NULL COMMENT '图片URL',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品图片表';

-- =============================================
-- 购物车表
-- =============================================
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `selected` TINYINT DEFAULT 1 COMMENT '是否选中 0-否 1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- =============================================
-- 订单表
-- =============================================
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `discount_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额',
    `freight_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '运费',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `pay_status` TINYINT DEFAULT 0 COMMENT '支付状态 0-未支付 1-已支付 2-已退款',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `order_status` TINYINT DEFAULT 0 COMMENT '订单状态 0-待支付 1-已支付 2-已发货 3-已完成 4-已取消',
    `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    `receive_time` DATETIME DEFAULT NULL COMMENT '收货时间',
    `address_id` BIGINT DEFAULT NULL COMMENT '收货地址ID',
    `address_snapshot` VARCHAR(1000) DEFAULT NULL COMMENT '收货地址快照(JSON)',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `coupon_id` BIGINT DEFAULT NULL COMMENT '优惠券ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_status` (`order_status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_status_time` (`user_id`, `order_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =============================================
-- 订单物流字段扩展
-- =============================================
ALTER TABLE `order` ADD COLUMN `logistics_company` VARCHAR(100) DEFAULT NULL COMMENT '物流公司';
ALTER TABLE `order` ADD COLUMN `logistics_no` VARCHAR(100) DEFAULT NULL COMMENT '物流单号';
ALTER TABLE `order` ADD COLUMN `auto_confirm_deadline` DATETIME DEFAULT NULL COMMENT '自动确认收货截止时间';
ALTER TABLE `order` ADD COLUMN `expire_time` DATETIME DEFAULT NULL COMMENT '支付锁定截止时间';

-- =============================================
-- 退款记录表
-- =============================================
CREATE TABLE IF NOT EXISTS `refund` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `refund_no` VARCHAR(50) NOT NULL COMMENT '退款单号',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '退款原因',
    `images` VARCHAR(1000) DEFAULT NULL COMMENT '凭证图片',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0-待审核 1-审核通过 2-退款中 3-已退款 4-拒绝 5-退货中 6-换货中',
    `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `payment_no` VARCHAR(50) DEFAULT NULL COMMENT '原支付单号',
    `retry_count` INT NOT NULL DEFAULT 0,
    `last_error` VARCHAR(500) DEFAULT NULL,
    `type` TINYINT DEFAULT 0 COMMENT '售后类型 0-仅退款 1-退货 2-换货',
    `logistics_company` VARCHAR(50) DEFAULT NULL COMMENT '退货物流公司',
    `logistics_no` VARCHAR(50) DEFAULT NULL COMMENT '退货物流单号',
    `return_address` VARCHAR(500) DEFAULT NULL COMMENT '退货地址（JSON格式）',
    `tracking_no` VARCHAR(50) DEFAULT NULL COMMENT '换货物流单号（发出新品）',
    `exchange_product_id` BIGINT DEFAULT NULL COMMENT '换货目标商品ID',
    `exchange_sku_id` BIGINT DEFAULT NULL COMMENT '换货目标SKU ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_refund_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录表';

-- =============================================
-- 评价表扩展(支持追评)
-- =============================================
ALTER TABLE `review` ADD COLUMN `parent_id` BIGINT DEFAULT 0 COMMENT '父级评价ID 0表示主评价';
ALTER TABLE `review` ADD COLUMN `reply_status` TINYINT DEFAULT 0 COMMENT '回复状态 0-未回复 1-已回复';

-- =============================================
-- 订单明细表
-- =============================================
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称快照',
    `sku_info` VARCHAR(500) DEFAULT NULL COMMENT '规格信息快照',
    `product_image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片快照',
    `price` DECIMAL(10,2) NOT NULL COMMENT '购买价格',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- =============================================
-- 收货地址表
-- =============================================
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
    `province` VARCHAR(50) NOT NULL COMMENT '省份',
    `city` VARCHAR(50) NOT NULL COMMENT '城市',
    `district` VARCHAR(50) NOT NULL COMMENT '区/县',
    `detail_address` VARCHAR(200) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认 0-否 1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- =============================================
-- 支付记录表
-- =============================================
DROP TABLE IF EXISTS `payment`;
CREATE TABLE `payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
    `payment_no` VARCHAR(50) NOT NULL COMMENT '支付单号',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `payment_method` TINYINT DEFAULT 1 COMMENT '支付方式 1-微信 2-支付宝 3-银行卡',
    `payment_status` TINYINT DEFAULT 0 COMMENT '支付状态 0-未支付 1-已支付 2-已退款',
    `payment_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_payment_order_status` (`order_id`, `payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- =============================================
-- 商品评价表
-- =============================================
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `rating` TINYINT NOT NULL COMMENT '评分 1-5星',
    `content` VARCHAR(1000) DEFAULT NULL COMMENT '评价内容',
    `images` VARCHAR(1000) DEFAULT NULL COMMENT '评价图片(JSON数组)',
    `reply` VARCHAR(500) DEFAULT NULL COMMENT '商家回复',
    `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-隐藏 1-显示',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品评价表';

-- =============================================
-- 收藏分组表
-- =============================================
DROP TABLE IF EXISTS `favorite_group`;
CREATE TABLE `favorite_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分组名称',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏分组表';

-- =============================================
-- 收藏表
-- =============================================
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `group_id` BIGINT DEFAULT NULL COMMENT '分组ID',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '收藏时的价格',
    `price_alert` TINYINT DEFAULT 1 COMMENT '降价提醒 0-关闭 1-开启',
    `stock_alert` TINYINT DEFAULT 0 COMMENT '到货提醒 0-关闭 1-开启',
    `last_price` DECIMAL(10,2) DEFAULT NULL COMMENT '最后检查价格',
    `last_stock` INT DEFAULT NULL COMMENT '最后检查库存',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_user_group` (`user_id`, `group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- =============================================
-- 浏览历史表
-- =============================================
DROP TABLE IF EXISTS `browse_history`;
CREATE TABLE `browse_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `browse_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览历史表';

-- =============================================
-- 首页轮播图表
-- =============================================
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
    `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
    `link_url` VARCHAR(500) DEFAULT NULL COMMENT '跳转链接',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页轮播图表';

-- =============================================
-- 优惠券表
-- =============================================
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    `type` TINYINT NOT NULL COMMENT '类型 1-满减 2-折扣 3-固定金额',
    `value` DECIMAL(10,2) NOT NULL COMMENT '优惠金额/折扣率',
    `min_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '最低消费金额',
    `total_count` INT NOT NULL COMMENT '发放总量',
    `remain_count` INT NOT NULL COMMENT '剩余数量',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';

-- =============================================
-- 用户优惠券表
-- =============================================
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0-未使用 1-已使用 2-已过期',
    `use_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `order_id` BIGINT DEFAULT NULL COMMENT '使用的订单ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- =============================================
-- 操作日志表
-- =============================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `admin_id` BIGINT DEFAULT NULL COMMENT '管理员ID',
    `admin_name` VARCHAR(50) DEFAULT NULL COMMENT '管理员名称',
    `module` VARCHAR(50) DEFAULT NULL COMMENT '模块',
    `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作内容',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-失败 1-成功',
    `cost_time` BIGINT DEFAULT NULL COMMENT '耗时(毫秒)',
    `event_type` VARCHAR(30) DEFAULT NULL COMMENT '业务事件类型',
    `user_coupon_id` BIGINT DEFAULT NULL COMMENT '用户优惠券ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `coupon_id` BIGINT DEFAULT NULL COMMENT '优惠券ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '订单ID',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '业务说明',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_operation_event_type` (`event_type`),
    KEY `idx_operation_user_coupon` (`user_coupon_id`),
    KEY `idx_operation_user_id` (`user_id`),
    KEY `idx_operation_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =============================================
-- 用户站内消息表
-- =============================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '接收消息的用户ID',
    `type` VARCHAR(30) DEFAULT NULL COMMENT '消息类型',
    `title` VARCHAR(120) NOT NULL COMMENT '消息标题',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '消息内容',
    `business_type` VARCHAR(40) DEFAULT NULL COMMENT '关联业务类型',
    `business_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `read_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_notification_user_read` (`user_id`, `is_read`),
    KEY `idx_notification_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户站内消息';

-- =============================================
-- 地区表(省市区)
-- =============================================
DROP TABLE IF EXISTS `region`;
DROP TABLE IF EXISTS `points_account`;
CREATE TABLE `points_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `balance` INT NOT NULL DEFAULT 0,
    `total_earned` INT NOT NULL DEFAULT 0,
    `total_spent` INT NOT NULL DEFAULT 0,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_points_account_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分账户';

DROP TABLE IF EXISTS `points_ledger`;
CREATE TABLE `points_ledger` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `amount` INT NOT NULL,
    `balance_after` INT NOT NULL,
    `event_type` VARCHAR(30) NOT NULL,
    `remark` VARCHAR(255) DEFAULT NULL,
    `business_id` BIGINT DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_points_ledger_user_time` (`user_id`, `create_time`),
    UNIQUE KEY `uk_points_ledger_event_business` (`event_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

DROP TABLE IF EXISTS `points_checkin`;
CREATE TABLE `points_checkin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `checkin_date` DATE NOT NULL,
    `points` INT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_points_checkin_user_date` (`user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日签到记录';

-- 积分兑换商品及兑换记录
DROP TABLE IF EXISTS `points_redemption`;
DROP TABLE IF EXISTS `points_product`;
CREATE TABLE `points_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(120) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `points_cost` INT NOT NULL,
    `stock` INT NOT NULL DEFAULT 0,
    `reward_type` VARCHAR(30) NOT NULL,
    `reward_value` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_points_product_status` (`status`, `stock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分兑换商品';

CREATE TABLE `points_redemption` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `points` INT NOT NULL,
    `redemption_code` VARCHAR(64) NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_points_redemption_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分兑换记录';

CREATE TABLE `region` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID 0表示顶级',
    `name` VARCHAR(50) NOT NULL COMMENT '名称',
    `level` TINYINT NOT NULL COMMENT '层级 1-省 2-市 3-区县',
    `sort` INT DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';

-- 省份 (level=1)
INSERT INTO `region` (`id`, `parent_id`, `name`, `level`, `sort`) VALUES
(1, 0, '北京市', 1, 1),
(2, 0, '上海市', 1, 2),
(3, 0, '广东省', 1, 3),
(4, 0, '浙江省', 1, 4),
(5, 0, '江苏省', 1, 5),
(6, 0, '四川省', 1, 6),
(7, 0, '湖北省', 1, 7),
(8, 0, '山东省', 1, 8),
(9, 0, '福建省', 1, 9),
(10, 0, '湖南省', 1, 10),
(11, 0, '河南省', 1, 11),
(12, 0, '河北省', 1, 12),
(13, 0, '安徽省', 1, 13),
(14, 0, '陕西省', 1, 14),
(15, 0, '辽宁省', 1, 15);

-- 城市 (level=2)
INSERT INTO `region` (`id`, `parent_id`, `name`, `level`, `sort`) VALUES
(101, 1, '北京市', 2, 1),
(201, 2, '上海市', 2, 1),
(301, 3, '广州市', 2, 1), (302, 3, '深圳市', 2, 2),
(401, 4, '杭州市', 2, 1), (402, 4, '宁波市', 2, 2),
(501, 5, '南京市', 2, 1), (502, 5, '苏州市', 2, 2),
(601, 6, '成都市', 2, 1),
(701, 7, '武汉市', 2, 1),
(801, 8, '济南市', 2, 1), (802, 8, '青岛市', 2, 2),
(901, 9, '福州市', 2, 1), (902, 9, '厦门市', 2, 2),
(1001, 10, '长沙市', 2, 1),
(1101, 11, '郑州市', 2, 1),
(1201, 12, '石家庄市', 2, 1),
(1301, 13, '合肥市', 2, 1),
(1401, 14, '西安市', 2, 1),
(1501, 15, '沈阳市', 2, 1);

-- 区县 (level=3)
INSERT INTO `region` (`id`, `parent_id`, `name`, `level`, `sort`) VALUES
-- 北京市 (101)
(10101, 101, '东城区', 3, 1), (10102, 101, '西城区', 3, 2), (10103, 101, '朝阳区', 3, 3),
(10104, 101, '海淀区', 3, 4), (10105, 101, '丰台区', 3, 5), (10106, 101, '石景山区', 3, 6),
(10107, 101, '通州区', 3, 7), (10108, 101, '昌平区', 3, 8), (10109, 101, '大兴区', 3, 9),
-- 上海市 (201)
(20101, 201, '黄浦区', 3, 1), (20102, 201, '徐汇区', 3, 2), (20103, 201, '长宁区', 3, 3),
(20104, 201, '静安区', 3, 4), (20105, 201, '普陀区', 3, 5), (20106, 201, '虹口区', 3, 6),
(20107, 201, '浦东新区', 3, 7), (20108, 201, '闵行区', 3, 8), (20109, 201, '宝山区', 3, 9),
-- 广州市 (301)
(30101, 301, '越秀区', 3, 1), (30102, 301, '海珠区', 3, 2), (30103, 301, '荔湾区', 3, 3),
(30104, 301, '天河区', 3, 4), (30105, 301, '白云区', 3, 5), (30106, 301, '黄埔区', 3, 6),
(30107, 301, '番禺区', 3, 7), (30108, 301, '花都区', 3, 8),
-- 深圳市 (302)
(30201, 302, '福田区', 3, 1), (30202, 302, '罗湖区', 3, 2), (30203, 302, '南山区', 3, 3),
(30204, 302, '宝安区', 3, 4), (30205, 302, '龙岗区', 3, 5), (30206, 302, '盐田区', 3, 6),
(30207, 302, '龙华区', 3, 7), (30208, 302, '坪山区', 3, 8),
-- 杭州市 (401)
(40101, 401, '上城区', 3, 1), (40102, 401, '拱墅区', 3, 2), (40103, 401, '西湖区', 3, 3),
(40104, 401, '滨江区', 3, 4), (40105, 401, '萧山区', 3, 5), (40106, 401, '余杭区', 3, 6),
(40107, 401, '临平区', 3, 7), (40108, 401, '钱塘区', 3, 8),
-- 宁波市 (402)
(40201, 402, '海曙区', 3, 1), (40202, 402, '江北区', 3, 2), (40203, 402, '北仑区', 3, 3),
(40204, 402, '镇海区', 3, 4), (40205, 402, '鄞州区', 3, 5),
-- 南京市 (501)
(50101, 501, '玄武区', 3, 1), (50102, 501, '秦淮区', 3, 2), (50103, 501, '建邺区', 3, 3),
(50104, 501, '鼓楼区', 3, 4), (50105, 501, '栖霞区', 3, 5), (50106, 501, '雨花台区', 3, 6),
(50107, 501, '江宁区', 3, 7),
-- 苏州市 (502)
(50201, 502, '姑苏区', 3, 1), (50202, 502, '虎丘区', 3, 2), (50203, 502, '吴中区', 3, 3),
(50204, 502, '相城区', 3, 4), (50205, 502, '工业园区', 3, 5),
-- 成都市 (601)
(60101, 601, '锦江区', 3, 1), (60102, 601, '青羊区', 3, 2), (60103, 601, '金牛区', 3, 3),
(60104, 601, '武侯区', 3, 4), (60105, 601, '成华区', 3, 5), (60106, 601, '龙泉驿区', 3, 6),
(60107, 601, '高新区', 3, 7), (60108, 601, '天府新区', 3, 8),
-- 武汉市 (701)
(70101, 701, '江岸区', 3, 1), (70102, 701, '江汉区', 3, 2), (70103, 701, '硚口区', 3, 3),
(70104, 701, '汉阳区', 3, 4), (70105, 701, '武昌区', 3, 5), (70106, 701, '洪山区', 3, 6),
(70107, 701, '东西湖区', 3, 7),
-- 济南市 (801)
(80101, 801, '历下区', 3, 1), (80102, 801, '市中区', 3, 2), (80103, 801, '槐荫区', 3, 3),
(80104, 801, '天桥区', 3, 4), (80105, 801, '历城区', 3, 5), (80106, 801, '长清区', 3, 6),
-- 青岛市 (802)
(80201, 802, '市南区', 3, 1), (80202, 802, '市北区', 3, 2), (80203, 802, '黄岛区', 3, 3),
(80204, 802, '崂山区', 3, 4), (80205, 802, '李沧区', 3, 5),
-- 福州市 (901)
(90101, 901, '鼓楼区', 3, 1), (90102, 901, '台江区', 3, 2), (90103, 901, '仓山区', 3, 3),
(90104, 901, '马尾区', 3, 4), (90105, 901, '晋安区', 3, 5),
-- 厦门市 (902)
(90201, 902, '思明区', 3, 1), (90202, 902, '海沧区', 3, 2), (90203, 902, '湖里区', 3, 3),
(90204, 902, '集美区', 3, 4), (90205, 902, '同安区', 3, 5),
-- 长沙市 (1001)
(100101, 1001, '芙蓉区', 3, 1), (100102, 1001, '天心区', 3, 2), (100103, 1001, '岳麓区', 3, 3),
(100104, 1001, '开福区', 3, 4), (100105, 1001, '雨花区', 3, 5), (100106, 1001, '望城区', 3, 6),
-- 郑州市 (1101)
(110101, 1101, '中原区', 3, 1), (110102, 1101, '二七区', 3, 2), (110103, 1101, '管城区', 3, 3),
(110104, 1101, '金水区', 3, 4), (110105, 1101, '上街区', 3, 5), (110106, 1101, '惠济区', 3, 6),
-- 石家庄市 (1201)
(120101, 1201, '长安区', 3, 1), (120102, 1201, '桥西区', 3, 2), (120103, 1201, '新华区', 3, 3),
(120104, 1201, '井陉矿区', 3, 4), (120105, 1201, '裕华区', 3, 5),
-- 合肥市 (1301)
(130101, 1301, '瑶海区', 3, 1), (130102, 1301, '庐阳区', 3, 2), (130103, 1301, '蜀山区', 3, 3),
(130104, 1301, '包河区', 3, 4), (130105, 1301, '高新区', 3, 5),
-- 西安市 (1401)
(140101, 1401, '新城区', 3, 1), (140102, 1401, '碑林区', 3, 2), (140103, 1401, '莲湖区', 3, 3),
(140104, 1401, '灞桥区', 3, 4), (140105, 1401, '未央区', 3, 5), (140106, 1401, '雁塔区', 3, 6),
-- 沈阳市 (1501)
(150101, 1501, '和平区', 3, 1), (150102, 1501, '沈河区', 3, 2), (150103, 1501, '大东区', 3, 3),
(150104, 1501, '皇姑区', 3, 4), (150105, 1501, '浑南区', 3, 5);

-- =============================================
-- 初始化数据
-- =============================================

-- 默认管理员 (密码: admin123, salt会在启动时自动生成并更新)
INSERT INTO `admin` (`id`, `username`, `password`, `salt`, `real_name`, `status`) VALUES
(1, 'admin', '', '', '超级管理员', 1);

-- 默认角色
INSERT INTO `role` (`id`, `name`, `code`, `description`, `sort`, `status`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限', 0, 1),
(2, '商品管理员', 'PRODUCT_ADMIN', '管理商品相关', 1, 1),
(3, '订单管理员', 'ORDER_ADMIN', '管理订单相关', 2, 1);

-- 默认权限(菜单)
INSERT INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `path`, `icon`, `sort`) VALUES
(1, 0, '首页', 'dashboard', 1, '/dashboard', 'Dashboard', 0),
(2, 0, '用户管理', 'user', 1, '/user', 'User', 1),
(3, 0, '商品管理', 'product', 1, '/product', 'Goods', 2),
(4, 0, '订单管理', 'order', 1, '/order', 'List', 3),
(5, 0, '评价管理', 'review', 1, '/review', 'ChatDotRound', 4),
(6, 0, '轮播图管理', 'banner', 1, '/banner', 'Picture', 5),
(7, 0, '优惠券管理', 'coupon', 1, '/coupon', 'Ticket', 6),
(8, 0, '系统管理', 'system', 1, '/system', 'Setting', 7),
(9, 8, '管理员管理', 'admin', 1, '/system/admin', 'User', 0),
(10, 8, '角色管理', 'role', 1, '/system/role', 'UserFilled', 1),
(11, 8, '菜单管理', 'menu', 1, '/system/menu', 'Menu', 2),
(12, 8, '操作日志', 'log', 1, '/system/log', 'Document', 3);

-- 默认权限(按钮/接口)
INSERT INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `path`) VALUES
(100, 2, '查看用户', 'user:view', 2, NULL),
(101, 2, '新增用户', 'user:add', 2, NULL),
(102, 2, '编辑用户', 'user:edit', 2, NULL),
(103, 2, '删除用户', 'user:delete', 2, NULL),
(200, 3, '查看商品', 'product:view', 2, NULL),
(201, 3, '新增商品', 'product:add', 2, NULL),
(202, 3, '编辑商品', 'product:edit', 2, NULL),
(203, 3, '删除商品', 'product:delete', 2, NULL),
(204, 3, '上架商品', 'product:onshelf', 2, NULL),
(205, 3, '下架商品', 'product:offshelf', 2, NULL),
(300, 4, '查看订单', 'order:view', 2, NULL),
(301, 4, '发货', 'order:ship', 2, NULL),
(302, 4, '修改订单状态', 'order:edit', 2, NULL);

-- 超级管理员拥有所有权限
INSERT INTO `admin_role` (`admin_id`, `role_id`) VALUES (1, 1);

-- 角色拥有权限
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(1, 9), (1, 10), (1, 11), (1, 12),
(1, 100), (1, 101), (1, 102), (1, 103),
(1, 200), (1, 201), (1, 202), (1, 203), (1, 204), (1, 205),
(1, 300), (1, 301), (1, 302);

-- =============================================
-- 营销活动主表
-- =============================================
CREATE TABLE IF NOT EXISTS `marketing_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '活动名称',
    `group_target` INT DEFAULT NULL COMMENT '拼团成团人数',
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

-- =============================================
-- 营销活动商品明细表
-- =============================================
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

-- =============================================
-- 营销活动参与者表（拼团/秒杀订单关联）
-- =============================================
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

-- =============================================
-- 会员等级表
-- =============================================
DROP TABLE IF EXISTS `member_level`;
CREATE TABLE `member_level` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `level` INT NOT NULL COMMENT '等级数值（1-初级, 2-中级, 3-高级, 4-VIP）',
    `min_points` INT NOT NULL DEFAULT 0 COMMENT '达到该等级所需最少积分',
    `max_points` INT NOT NULL DEFAULT 0 COMMENT '达到该等级所需最多积分（0表示无上限）',
    `points_rate` DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '积分倍率（1.0=原价1倍积分, 1.5=1.5倍积分）',
    `discount_rate` DECIMAL(3,2) DEFAULT 1.00 COMMENT '折扣率（0.9=9折, 1.0=无折扣）',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '等级图标',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '等级描述/权益说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_level_level` (`level`),
    KEY `idx_level_status` (`level`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级表';

-- 初始化默认等级数据
INSERT INTO `member_level` (`name`, `level`, `min_points`, `max_points`, `points_rate`, `discount_rate`, `icon`, `description`, `status`, `sort`) VALUES
('普通会员', 1, 0, 999, 1.00, 1.00, '🥉', '注册即为普通会员，享受基础积分权益', 1, 1),
('白银会员', 2, 1000, 4999, 1.20, 0.98, '🥈', '满1000积分升级，积分1.2倍，享98折', 1, 2),
('黄金会员', 3, 5000, 19999, 1.50, 0.95, '🥇', '满5000积分升级，积分1.5倍，享95折', 1, 3),
('钻石会员', 4, 20000, 0, 2.00, 0.90, '💎', '满20000积分升级，积分2倍，享9折优惠', 1, 4);

-- 用户表增加会员等级字段
ALTER TABLE `user` ADD COLUMN `member_level_id` BIGINT DEFAULT NULL COMMENT '会员等级ID' AFTER `deleted`;

-- 积分账户表增加会员等级字段
ALTER TABLE `points_account` ADD COLUMN `member_level_id` BIGINT DEFAULT NULL COMMENT '会员等级ID' AFTER `user_id`;
