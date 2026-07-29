-- 商城模拟数据脚本
-- 运行方式: mysql -u root -p mall < mock_data.sql

-- 清理旧数据（保留 admin 用户）
DELETE FROM `order_item`;
DELETE FROM `order`;
DELETE FROM `cart`;
DELETE FROM `user_coupon`;
DELETE FROM `coupon`;
DELETE FROM `product_sku`;
DELETE FROM `product_image`;
DELETE FROM `product`;
DELETE FROM `brand`;
DELETE FROM `category`;
DELETE FROM `banner` WHERE id > 1;
DELETE FROM `user` WHERE id > 1;

-- ============================================
-- 1. 分类数据
-- ============================================
INSERT INTO `category` (`id`, `name`, `parent_id`, `sort`, `icon`, `deleted`) VALUES
(1, '手机数码', NULL, 1, '📱', 0),
(2, '电脑办公', NULL, 2, '💻', 0),
(3, '家用电器', NULL, 3, '🏠', 0),
(4, '服装鞋包', NULL, 4, '👔', 0),
(5, '食品生鲜', NULL, 5, '🍎', 0),
(6, '智能手机', 1, 1, NULL, 0),
(7, '手机配件', 1, 2, NULL, 0),
(8, '笔记本电脑', 2, 1, NULL, 0),
(9, '外设配件', 2, 2, NULL, 0),
(10, '大家电', 3, 1, NULL, 0),
(11, '生活电器', 3, 2, NULL, 0);

-- ============================================
-- 2. 品牌数据
-- ============================================
INSERT INTO `brand` (`id`, `name`, `logo`, `description`, `sort`, `status`, `deleted`) VALUES
(1, '华为', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Huawei%20logo%20minimalist%20red%20flower&image_size=square', '全球领先的ICT基础设施和智能终端提供商', 1, 1, 0),
(2, '小米', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Xiaomi%20logo%20orange%20mi&image_size=square', '让每个人都能享受科技的乐趣', 2, 1, 0),
(3, '苹果', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Apple%20logo%20silver%20minimalist&image_size=square', '设计改变世界', 3, 1, 0),
(4, '联想', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Lenovo%20logo%20blue&image_size=square', '全球领先的个人科技公司', 4, 1, 0),
(5, '美的', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Midea%20logo%20blue%20home&image_size=square', '科技尽善，生活尽美', 5, 1, 0),
(6, '海尔', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Haier%20logo%20blue&image_size=square', '真诚到永远', 6, 1, 0),
(7, '耐克', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Nike%20logo%20black%20swoosh&image_size=square', 'Just Do It', 7, 1, 0),
(8, '阿迪达斯', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Adidas%20logo%20black%20three%20stripes&image_size=square', 'Impossible Is Nothing', 8, 1, 0);

-- ============================================
-- 3. 商品数据
-- ============================================
INSERT INTO `product` (`id`, `category_id`, `brand_id`, `name`, `subtitle`, `main_image`, `price`, `original_price`, `total_stock`, `sales`, `status`, `is_recommend`, `description`, `deleted`) VALUES
-- 智能手机
(1, 6, 1, '华为 Mate 60 Pro', '麒麟9000s处理器，北斗卫星消息', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Huawei%20Mate60%20Pro%20smartphone%20black%20premium&image_size=square', 6999.00, 7999.00, 500, 128, 1, 1, '<p>华为Mate 60 Pro，搭载麒麟9000s处理器，支持北斗卫星消息，鸿蒙4.0系统，88W有线快充，50W无线快充。</p>', 0),
(2, 6, 2, '小米14 Ultra', '徕卡光学，骁龙8 Gen3', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Xiaomi%2014%20Ultra%20smartphone%20white%20camera&image_size=square', 5999.00, 6499.00, 300, 85, 1, 1, '<p>小米14 Ultra，徕卡专业光学镜头，骁龙8 Gen3处理器，5000万像素主摄，90W有线快充。</p>', 0),
(3, 6, 3, 'iPhone 15 Pro Max', 'A17 Pro芯片，钛金属设计', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=iPhone%2015%20Pro%20Max%20natural%20titanium&image_size=square', 9999.00, 10999.00, 200, 256, 1, 1, '<p>iPhone 15 Pro Max，A17 Pro芯片，钛金属设计，4800万像素主摄，ProMotion显示屏。</p>', 0),
(4, 6, 1, '华为 nova 12', '前置6000万像素，鸿蒙4.0', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Huawei%20nova%2012%20smartphone%20blue%20youth&image_size=square', 2999.00, 3499.00, 800, 42, 1, 0, '<p>华为nova 12，前置6000万像素人像超广角，66W有线快充，鸿蒙4.0系统。</p>', 0),

-- 笔记本电脑
(5, 8, 4, '联想 ThinkPad X1 Carbon', '商务旗舰，轻薄便携', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Lenovo%20ThinkPad%20X1%20Carbon%20laptop%20black%20business&image_size=square', 10999.00, 12999.00, 100, 23, 1, 1, '<p>ThinkPad X1 Carbon，14英寸2.8K OLED屏幕，13代酷睿i7，16GB内存，512GB SSD。</p>', 0),
(6, 8, 3, 'MacBook Pro 14', 'M3 Pro芯片，专业生产力', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=MacBook%20Pro%2014%20silver%20apple%20laptop&image_size=square', 16999.00, 18999.00, 80, 67, 1, 1, '<p>MacBook Pro 14英寸，M3 Pro芯片，18GB统一内存，512GB固态硬盘，Liquid Retina XDR显示屏。</p>', 0),

-- 家用电器
(7, 10, 5, '美的 对开门冰箱', '520L大容量，一级能效', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Midea%20refrigerator%20silver%20double%20door&image_size=square', 4599.00, 5299.00, 150, 38, 1, 0, '<p>美的520L对开门冰箱，风冷无霜，一级能效，LED触摸显示屏。</p>', 0),
(8, 11, 6, '海尔 滚筒洗衣机', '10KG大容量，直驱变频', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Haier%20washing%20machine%20white%20front%20load&image_size=square', 2899.00, 3499.00, 200, 56, 1, 0, '<p>海尔10KG滚筒洗衣机，直驱变频电机，95°C高温煮洗，APP智能控制。</p>', 0),

-- 手机配件
(9, 7, 1, '华为 FreeBuds Pro 3', '旗舰降噪耳机，空间音频', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Huawei%20FreeBuds%20Pro%203%20earbuds%20white%20case&image_size=square', 1499.00, 1699.00, 600, 189, 1, 1, '<p>华为FreeBuds Pro 3，旗舰级主动降噪，空间音频，智慧生活互联。</p>', 0),
(10, 7, 2, '小米 Buds 4 Pro', '智能降噪，无线充电', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Xiaomi%20Buds%204%20Pro%20earbuds%20white&image_size=square', 999.00, 1199.00, 400, 145, 1, 0, '<p>小米Buds 4 Pro，48dB智能降噪，无线充电，12小时续航。</p>', 0);

-- ============================================
-- 4. 商品SKU
-- ============================================
INSERT INTO `product_sku` (`id`, `product_id`, `sku_code`, `spec_info`, `price`, `stock`, `image`, `deleted`) VALUES
-- Mate 60 Pro
(1, 1, 'M60P-256-BLK', '雅丹黑 256GB', 6999.00, 200, NULL, 0),
(2, 1, 'M60P-256-WHT', '雅川青 256GB', 6999.00, 150, NULL, 0),
(3, 1, 'M60P-512-BLK', '雅丹黑 512GB', 7999.00, 150, NULL, 0),
-- 小米14 Ultra
(4, 2, 'X14U-256-WHT', '白色 256GB', 5999.00, 100, NULL, 0),
(5, 2, 'X14U-512-BLK', '黑色 512GB', 6499.00, 100, NULL, 0),
(6, 2, 'X14U-512-WHT', '白色 512GB', 6499.00, 100, NULL, 0),
-- iPhone 15 Pro Max
(7, 3, 'IP15PM-256-NAT', '原色钛金属 256GB', 9999.00, 50, NULL, 0),
(8, 3, 'IP15PM-256-BLU', '蓝色钛金属 256GB', 9999.00, 50, NULL, 0),
(9, 3, 'IP15PM-512-NAT', '原色钛金属 512GB', 10999.00, 50, NULL, 0),
(10, 3, 'IP15PM-512-WHT', '白色钛金属 512GB', 10999.00, 50, NULL, 0),
-- 华为 nova 12
(11, 4, 'NV12-256-BLU', '12号色 256GB', 2999.00, 400, NULL, 0),
(12, 4, 'NV12-256-PNK', '樱语粉 256GB', 2999.00, 400, NULL, 0),
-- ThinkPad X1 Carbon
(13, 5, 'TP-X1-I7-16G', 'i7/16GB/512GB', 10999.00, 50, NULL, 0),
(14, 5, 'TP-X1-I7-32G', 'i7/32GB/1TB', 12999.00, 50, NULL, 0),
-- MacBook Pro 14
(15, 6, 'MBP14-M3P-18G', 'M3 Pro/18GB/512GB', 16999.00, 40, NULL, 0),
(16, 6, 'MBP14-M3P-36G', 'M3 Pro/36GB/1TB', 18999.00, 40, NULL, 0),
-- 冰箱
(17, 7, 'MD-RF520-SLV', '银色 520L', 4599.00, 150, NULL, 0),
-- 洗衣机
(18, 8, 'HR-WM10-WHT', '白色 10KG', 2899.00, 200, NULL, 0),
-- FreeBuds Pro 3
(19, 9, 'FBP3-WHT', '陶瓷白', 1499.00, 300, NULL, 0),
(20, 9, 'FBP3-BLK', '碳晶黑', 1499.00, 300, NULL, 0),
-- Buds 4 Pro
(21, 10, 'XB4P-WHT', '白色', 999.00, 200, NULL, 0),
(22, 10, 'XB4P-BLK', '黑色', 999.00, 200, NULL, 0);

-- ============================================
-- 5. 商品图片
-- ============================================
INSERT INTO `product_image` (`product_id`, `url`, `sort`, `deleted`) VALUES
(1, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Huawei%20Mate60%20Pro%20front%20view&image_size=square', 1, 0),
(1, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Huawei%20Mate60%20Pro%20back%20camera&image_size=square', 2, 0),
(2, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Xiaomi%2014%20Ultra%20front%20view&image_size=square', 1, 0),
(2, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Xiaomi%2014%20Ultra%20camera%20module&image_size=square', 2, 0),
(3, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=iPhone%2015%20Pro%20Max%20front&image_size=square', 1, 0),
(3, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=iPhone%2015%20Pro%20Max%20back%20titanium&image_size=square', 2, 0);

-- ============================================
-- 6. 用户数据 (密码都是 123456，BCrypt加密)
-- ============================================
INSERT INTO `user` (`id`, `username`, `phone`, `email`, `password`, `salt`, `avatar`, `nickname`, `gender`, `status`, `deleted`) VALUES
(2, 'zhangsan', '13800138001', 'zhangsan@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '张三', 1, 1, 0),
(3, 'lisi', '13800138002', 'lisi@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '李四', 1, 1, 0),
(4, 'wangwu', '13800138003', 'wangwu@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '王五', 1, 1, 0),
(5, 'zhaoliu', '13800138004', 'zhaoliu@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '赵六', 1, 1, 0),
(6, 'sunqi', '13800138005', 'sunqi@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '孙七', 2, 1, 0),
(7, 'zhouba', '13800138006', 'zhouba@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '周八', 1, 1, 0),
(8, 'wujiu', '13800138007', 'wujiu@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '吴九', 1, 1, 0),
(9, 'zhengshi', '13800138008', 'zhengshi@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '郑十', 2, 1, 0),
(10, 'buyer01', '13800138009', 'buyer01@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '买家01', 1, 1, 0),
(11, 'buyer02', '13800138010', 'buyer02@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '买家02', 2, 1, 0),
(12, 'buyer03', '13800138011', 'buyer03@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7eI2tP4JZxvJQjGJx5w0v7O', NULL, NULL, '买家03', 1, 1, 0);

-- ============================================
-- 7. 收货地址
-- ============================================
INSERT INTO `address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail_address`, `is_default`, `deleted`) VALUES
(1, 2, '张三', '13800138001', '北京市', '北京市', '朝阳区', '建国路88号SOHO现代城A座1501', 1, 0),
(2, 3, '李四', '13800138002', '上海市', '上海市', '浦东新区', '陆家嘴环路1000号恒生银行大厦', 1, 0),
(3, 4, '王五', '13800138003', '广东省', '深圳市', '南山区', '科技园南区深南大道10000号', 1, 0),
(4, 5, '赵六', '13800138004', '浙江省', '杭州市', '西湖区', '文三路398号东信大厦', 1, 0),
(5, 6, '孙七', '13800138005', '江苏省', '南京市', '鼓楼区', '汉中路2号', 1, 0),
(6, 7, '周八', '13800138006', '四川省', '成都市', '高新区', '天府大道北段1700号', 1, 0),
(7, 8, '吴九', '13800138007', '湖北省', '武汉市', '武昌区', '中南路1号', 1, 0),
(8, 9, '郑十', '13800138008', '陕西省', '西安市', '雁塔区', '小寨东路1号', 1, 0),
(9, 10, '买家01', '13800138009', '北京市', '北京市', '海淀区', '中关村大街1号', 1, 0),
(10, 11, '买家02', '13800138010', '上海市', '上海市', '徐汇区', '漕溪北路88号', 1, 0),
(11, 12, '买家03', '13800138011', '广东省', '广州市', '天河区', '天河路385号', 1, 0);

-- ============================================
-- 8. 订单数据（不同状态和日期）
-- ============================================

-- 过去7天每天的销售订单（状态为已完成，用于统计图表）
INSERT INTO `order` (`id`, `order_no`, `user_id`, `total_amount`, `discount_amount`, `freight_amount`, `pay_amount`, `pay_status`, `pay_time`, `order_status`, `ship_time`, `receive_time`, `address_id`, `address_snapshot`, `remark`, `coupon_id`, `create_time`, `update_time`, `deleted`) VALUES
-- 7天前 (2026-07-22)
(1, 'ORD202607220001', 2, 6999.00, 0.00, 0.00, 6999.00, 1, '2026-07-22 10:30:00', 3, '2026-07-23 09:00:00', '2026-07-25 15:00:00', 1, '{"receiverName":"张三","receiverPhone":"13800138001","province":"北京市","city":"北京市","district":"朝阳区","detailAddress":"建国路88号SOHO现代城A座1501"}', NULL, NULL, '2026-07-22 10:00:00', '2026-07-25 15:00:00', 0),
-- 6天前 (2026-07-23)
(2, 'ORD202607230001', 3, 5999.00, 0.00, 0.00, 5999.00, 1, '2026-07-23 14:00:00', 3, '2026-07-24 10:00:00', '2026-07-26 16:00:00', 2, '{"receiverName":"李四","receiverPhone":"13800138002","province":"上海市","city":"上海市","district":"浦东新区","detailAddress":"陆家嘴环路1000号恒生银行大厦"}', NULL, NULL, '2026-07-23 13:30:00', '2026-07-26 16:00:00', 0),
(3, 'ORD202607230002', 4, 9999.00, 0.00, 0.00, 9999.00, 1, '2026-07-23 16:00:00', 3, '2026-07-24 11:00:00', '2026-07-26 18:00:00', 3, '{"receiverName":"王五","receiverPhone":"13800138003","province":"广东省","city":"深圳市","district":"南山区","detailAddress":"科技园南区深南大道10000号"}', '周末送', NULL, '2026-07-23 15:45:00', '2026-07-26 18:00:00', 0),
-- 5天前 (2026-07-24)
(4, 'ORD202607240001', 5, 2899.00, 0.00, 0.00, 2899.00, 1, '2026-07-24 09:00:00', 3, '2026-07-25 14:00:00', '2026-07-27 10:00:00', 4, '{"receiverName":"赵六","receiverPhone":"13800138004","province":"浙江省","city":"杭州市","district":"西湖区","detailAddress":"文三路398号东信大厦"}', NULL, NULL, '2026-07-24 08:30:00', '2026-07-27 10:00:00', 0),
(5, 'ORD202607240002', 6, 10999.00, 0.00, 0.00, 10999.00, 1, '2026-07-24 11:00:00', 3, '2026-07-25 15:00:00', '2026-07-27 14:00:00', 5, '{"receiverName":"孙七","receiverPhone":"13800138005","province":"江苏省","city":"南京市","district":"鼓楼区","detailAddress":"汉中路2号"}', '开发票', NULL, '2026-07-24 10:30:00', '2026-07-27 14:00:00', 0),
-- 4天前 (2026-07-25)
(6, 'ORD202607250001', 7, 1499.00, 0.00, 0.00, 1499.00, 1, '2026-07-25 10:00:00', 3, '2026-07-26 09:00:00', '2026-07-28 11:00:00', 6, '{"receiverName":"周八","receiverPhone":"13800138006","province":"四川省","city":"成都市","district":"高新区","detailAddress":"天府大道北段1700号"}', NULL, NULL, '2026-07-25 09:30:00', '2026-07-28 11:00:00', 0),
(7, 'ORD202607250002', 8, 4599.00, 0.00, 0.00, 4599.00, 1, '2026-07-25 15:00:00', 3, '2026-07-26 14:00:00', '2026-07-28 16:00:00', 7, '{"receiverName":"吴九","receiverPhone":"13800138007","province":"湖北省","city":"武汉市","district":"武昌区","detailAddress":"中南路1号"}', NULL, NULL, '2026-07-25 14:30:00', '2026-07-28 16:00:00', 0),
-- 3天前 (2026-07-26)
(8, 'ORD202607260001', 9, 16999.00, 0.00, 0.00, 16999.00, 1, '2026-07-26 10:00:00', 3, '2026-07-27 09:00:00', '2026-07-28 10:00:00', 8, '{"receiverName":"郑十","receiverPhone":"13800138008","province":"陕西省","city":"西安市","district":"雁塔区","detailAddress":"小寨东路1号"}', '贵重物品', NULL, '2026-07-26 09:30:00', '2026-07-28 10:00:00', 0),
(9, 'ORD202607260002', 10, 6999.00, 0.00, 0.00, 6999.00, 1, '2026-07-26 14:00:00', 3, '2026-07-27 10:00:00', '2026-07-28 15:00:00', 9, '{"receiverName":"买家01","receiverPhone":"13800138009","province":"北京市","city":"北京市","district":"海淀区","detailAddress":"中关村大街1号"}', NULL, NULL, '2026-07-26 13:30:00', '2026-07-28 15:00:00', 0),
-- 2天前 (2026-07-27)
(10, 'ORD202607270001', 11, 999.00, 0.00, 0.00, 999.00, 1, '2026-07-27 09:00:00', 3, '2026-07-28 10:00:00', NULL, 10, '{"receiverName":"买家02","receiverPhone":"13800138010","province":"上海市","city":"上海市","district":"徐汇区","detailAddress":"漕溪北路88号"}', NULL, NULL, '2026-07-27 08:30:00', '2026-07-28 10:00:00', 0),
(11, 'ORD202607270002', 12, 2999.00, 0.00, 0.00, 2999.00, 1, '2026-07-27 11:00:00', 2, '2026-07-27 17:00:00', NULL, 11, '{"receiverName":"买家03","receiverPhone":"13800138011","province":"广东省","city":"广州市","district":"天河区","detailAddress":"天河路385号"}', NULL, NULL, '2026-07-27 10:30:00', '2026-07-27 17:00:00', 0),
(12, 'ORD202607270003', 2, 5999.00, 0.00, 0.00, 5999.00, 1, '2026-07-27 16:00:00', 2, '2026-07-28 09:00:00', NULL, 1, '{"receiverName":"张三","receiverPhone":"13800138001","province":"北京市","city":"北京市","district":"朝阳区","detailAddress":"建国路88号SOHO现代城A座1501"}', NULL, NULL, '2026-07-27 15:30:00', '2026-07-28 09:00:00', 0),
-- 昨天 (2026-07-28)
(13, 'ORD202607280001', 3, 6499.00, 0.00, 0.00, 6499.00, 1, '2026-07-28 10:00:00', 1, '2026-07-28 10:05:00', NULL, 2, '{"receiverName":"李四","receiverPhone":"13800138002","province":"上海市","city":"上海市","district":"浦东新区","detailAddress":"陆家嘴环路1000号恒生银行大厦"}', NULL, NULL, '2026-07-28 09:30:00', '2026-07-28 10:05:00', 0),
(14, 'ORD202607280002', 4, 10999.00, 0.00, 0.00, 10999.00, 1, '2026-07-28 14:00:00', 1, '2026-07-28 14:05:00', NULL, 3, '{"receiverName":"王五","receiverPhone":"13800138003","province":"广东省","city":"深圳市","district":"南山区","detailAddress":"科技园南区深南大道10000号"}', '加急', NULL, '2026-07-28 13:30:00', '2026-07-28 14:05:00', 0),
(15, 'ORD202607280003', 5, 1499.00, 0.00, 0.00, 1499.00, 0, NULL, 0, NULL, NULL, 4, '{"receiverName":"赵六","receiverPhone":"13800138004","province":"浙江省","city":"杭州市","district":"西湖区","detailAddress":"文三路398号东信大厦"}', '尽快发货', NULL, '2026-07-28 15:00:00', '2026-07-28 15:00:00', 0);

-- 订单项
INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `sku_id`, `product_name`, `sku_info`, `product_image`, `price`, `quantity`, `subtotal`, `deleted`) VALUES
(1, 1, 1, 1, '华为 Mate 60 Pro', '雅丹黑 256GB', NULL, 6999.00, 1, 6999.00, 0),
(2, 2, 2, 4, '小米14 Ultra', '白色 256GB', NULL, 5999.00, 1, 5999.00, 0),
(3, 3, 3, 7, 'iPhone 15 Pro Max', '原色钛金属 256GB', NULL, 9999.00, 1, 9999.00, 0),
(4, 4, 8, 18, '海尔 滚筒洗衣机', '白色 10KG', NULL, 2899.00, 1, 2899.00, 0),
(5, 5, 5, 13, '联想 ThinkPad X1 Carbon', 'i7/16GB/512GB', NULL, 10999.00, 1, 10999.00, 0),
(6, 6, 9, 19, '华为 FreeBuds Pro 3', '陶瓷白', NULL, 1499.00, 1, 1499.00, 0),
(7, 7, 7, 17, '美的 对开门冰箱', '银色 520L', NULL, 4599.00, 1, 4599.00, 0),
(8, 8, 6, 15, 'MacBook Pro 14', 'M3 Pro/18GB/512GB', NULL, 16999.00, 1, 16999.00, 0),
(9, 9, 1, 2, '华为 Mate 60 Pro', '雅川青 256GB', NULL, 6999.00, 1, 6999.00, 0),
(10, 10, 10, 21, '小米 Buds 4 Pro', '白色', NULL, 999.00, 1, 999.00, 0),
(11, 11, 4, 11, '华为 nova 12', '12号色 256GB', NULL, 2999.00, 1, 2999.00, 0),
(12, 12, 2, 5, '小米14 Ultra', '黑色 512GB', NULL, 6499.00, 1, 6499.00, 0),
(13, 13, 2, 6, '小米14 Ultra', '白色 512GB', NULL, 6499.00, 1, 6499.00, 0),
(14, 14, 5, 14, '联想 ThinkPad X1 Carbon', 'i7/32GB/1TB', NULL, 12999.00, 1, 12999.00, 0),
(15, 15, 9, 20, '华为 FreeBuds Pro 3', '碳晶黑', NULL, 1499.00, 1, 1499.00, 0);

-- ============================================
-- 9. Banner数据
-- ============================================
INSERT INTO `banner` (`id`, `title`, `image_url`, `link_url`, `sort`, `status`, `start_time`, `end_time`, `deleted`) VALUES
(1, '华为新品首发', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Huawei%20Mate60%20banner%20premium%20black%20gold&image_size=landscape_16_9', '/products/1', 1, 1, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 0),
(2, '小米14 Ultra 震撼上市', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Xiaomi%2014%20Ultra%20banner%20white%20camera&image_size=landscape_16_9', '/products/2', 2, 1, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 0),
(3, 'iPhone 15 Pro 系列特惠', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=iPhone%2015%20Pro%20banner%20titanium%20blue&image_size=landscape_16_9', '/products/3', 3, 1, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 0),
(4, '数码好物 爆款直降', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Electronics%20sale%20banner%20blue%20tech&image_size=landscape_16_9', '/products', 4, 1, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 0);

-- ============================================
-- 10. 优惠券数据
-- ============================================
INSERT INTO `coupon` (`id`, `name`, `type`, `value`, `min_amount`, `total_count`, `remain_count`, `start_time`, `end_time`, `status`, `description`, `deleted`) VALUES
(1, '新人专享券', 1, 50.00, 299.00, 1000, 850, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '新用户首单立减50元', 0),
(2, '满500减30', 1, 30.00, 500.00, 500, 420, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '全场通用，满500减30', 0),
(3, '满1000减100', 1, 100.00, 1000.00, 300, 280, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '大额优惠，满1000减100', 0),
(4, '数码专享券', 1, 200.00, 3000.00, 200, 150, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '数码品类专享，满3000减200', 0),
(5, '家电优惠券', 1, 150.00, 2000.00, 150, 120, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '家电品类专享，满2000减150', 0);

-- 用户优惠券
INSERT INTO `user_coupon` (`id`, `user_id`, `coupon_id`, `status`, `receive_time`, `deleted`) VALUES
(1, 2, 1, 0, '2024-07-20 10:00:00', 0),
(2, 2, 2, 0, '2024-07-20 10:00:00', 0),
(3, 3, 1, 0, '2024-07-21 14:00:00', 0),
(4, 4, 3, 0, '2024-07-22 09:00:00', 0),
(5, 5, 4, 0, '2024-07-22 16:00:00', 0),
(6, 6, 2, 0, '2024-07-23 11:00:00', 0),
(7, 7, 5, 0, '2024-07-24 10:00:00', 0),
(8, 8, 3, 0, '2024-07-24 15:00:00', 0);

-- ============================================
-- 11. 评价数据（用于待处理评价统计）
-- ============================================
INSERT INTO `review` (`id`, `user_id`, `product_id`, `order_id`, `rating`, `content`, `images`, `status`, `reply`, `reply_time`, `deleted`) VALUES
(1, 2, 1, 1, 5, '非常好用，续航给力，信号也强！', NULL, 0, NULL, NULL, 0),
(2, 3, 2, 2, 5, '徕卡拍照太棒了，画质无敌', NULL, 0, NULL, NULL, 0),
(3, 4, 3, 3, 4, '手感很好，但价格略贵', NULL, 0, NULL, NULL, 0),
(4, 5, 8, 4, 5, '洗衣机很安静，洗得干净', NULL, 0, NULL, NULL, 0),
(5, 6, 5, 5, 5, '键盘手感一流，商务首选', NULL, 0, NULL, NULL, 0);

SELECT '模拟数据导入完成！' AS message;