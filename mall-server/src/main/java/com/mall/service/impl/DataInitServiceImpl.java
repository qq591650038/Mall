package com.mall.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.entity.*;
import com.mall.mapper.*;
import com.mall.service.DataInitService;
import com.mall.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class DataInitServiceImpl implements DataInitService {

    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductImageMapper imageMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final BannerMapper bannerMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final ReviewMapper reviewMapper;
    private final PasswordUtil passwordUtil;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public DataInitServiceImpl(CategoryMapper categoryMapper,
                                BrandMapper brandMapper,
                                ProductMapper productMapper,
                                ProductSkuMapper skuMapper,
                                ProductImageMapper imageMapper,
                                UserMapper userMapper,
                                AddressMapper addressMapper,
                                OrderMapper orderMapper,
                                OrderItemMapper orderItemMapper,
                                BannerMapper bannerMapper,
                                CouponMapper couponMapper,
                                UserCouponMapper userCouponMapper,
                                ReviewMapper reviewMapper,
                                PasswordUtil passwordUtil,
                                ObjectMapper objectMapper,
                                JdbcTemplate jdbcTemplate) {
        this.categoryMapper = categoryMapper;
        this.brandMapper = brandMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.imageMapper = imageMapper;
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.bannerMapper = bannerMapper;
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.reviewMapper = reviewMapper;
        this.passwordUtil = passwordUtil;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initMockData() {
        log.info("开始初始化模拟数据...");

        // 清理旧数据
        cleanOldData();

        // 创建基础数据
        createCategories();
        createBrands();
        createProducts();
        createUsers();
        createAddresses();
        createOrders();
        createBanners();
        createCoupons();
        createReviews();

        log.info("模拟数据初始化完成！");
    }

    private void cleanOldData() {
        log.info("清理旧数据...");
        jdbcTemplate.execute("DELETE FROM review");
        jdbcTemplate.execute("DELETE FROM order_item");
        jdbcTemplate.execute("DELETE FROM `order`");
        jdbcTemplate.execute("DELETE FROM user_coupon");
        jdbcTemplate.execute("DELETE FROM coupon");
        jdbcTemplate.execute("DELETE FROM banner");
        jdbcTemplate.execute("DELETE FROM address");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM product_sku");
        jdbcTemplate.execute("DELETE FROM product_image");
        jdbcTemplate.execute("DELETE FROM product");
        jdbcTemplate.execute("DELETE FROM brand");
        jdbcTemplate.execute("DELETE FROM category");
    }

    private void createCategories() {
        log.info("创建分类数据...");
        List<Category> categories = List.of(
                createCategory(1L, "手机数码", null, 1, "📱"),
                createCategory(2L, "电脑办公", null, 2, "💻"),
                createCategory(3L, "家用电器", null, 3, "🏠"),
                createCategory(4L, "服装鞋包", null, 4, "👔"),
                createCategory(6L, "智能手机", 1L, 1, null),
                createCategory(7L, "手机配件", 1L, 2, null),
                createCategory(8L, "笔记本电脑", 2L, 1, null),
                createCategory(9L, "外设配件", 2L, 2, null),
                createCategory(10L, "大家电", 3L, 1, null),
                createCategory(11L, "生活电器", 3L, 2, null),
                createCategory(12L, "服装", 4L, 1, null),
                createCategory(13L, "鞋靴", 4L, 2, null),
                createCategory(14L, "箱包", 4L, 3, null)
        );
        categories.forEach(categoryMapper::insert);
    }

    private Category createCategory(Long id, String name, Long parentId, Integer sort, String icon) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setParentId(parentId);
        c.setSort(sort);
        c.setIcon(icon);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        c.setDeleted(0);
        return c;
    }

    private void createBrands() {
        log.info("创建品牌数据...");
        List<Brand> brands = List.of(
                createBrand(1L, "华为", "全球领先的ICT基础设施和智能终端提供商", 1),
                createBrand(2L, "小米", "让每个人都能享受科技的乐趣", 2),
                createBrand(3L, "苹果", "设计改变世界", 3),
                createBrand(4L, "联想", "全球领先的个人科技公司", 4),
                createBrand(5L, "美的", "科技尽善，生活尽美", 5),
                createBrand(6L, "海尔", "真诚到永远", 6),
                createBrand(7L, "耐克", "Just Do It", 7),
                createBrand(8L, "阿迪达斯", "Impossible Is Nothing", 8),
                createBrand(9L, "荣耀", "HONOR 荣耀前行", 9),
                createBrand(10L, "一加", "OnePlus 不将就", 10),
                createBrand(11L, "vivo", "vivo 乐享非凡", 11),
                createBrand(12L, "海澜之家", "HLA 男人的衣柜", 12),
                createBrand(13L, "啄木鸟", "TUCANO 温暖如春", 13),
                createBrand(14L, "安踏", "ANTA 永不止步", 14),
                createBrand(15L, "戴森", "Dyson 创新科技", 15),
                createBrand(16L, "飞利浦", "Philips 精于心简于形", 16)
        );
        brands.forEach(brandMapper::insert);
    }

    private Brand createBrand(Long id, String name, String desc, Integer sort) {
        Brand b = new Brand();
        b.setId(id);
        b.setName(name);
        b.setDescription(desc);
        b.setSort(sort);
        b.setStatus(1);
        b.setCreateTime(LocalDateTime.now());
        b.setUpdateTime(LocalDateTime.now());
        b.setDeleted(0);
        return b;
    }

    private void createProducts() {
        log.info("创建商品数据...");
        List<Product> products = List.of(
                createProduct(1L, 6L, 1L, "华为 Mate 60 Pro", "麒麟9000s处理器，北斗卫星消息", new BigDecimal("6999"), new BigDecimal("7999"), 500, 128, 1, true),
                createProduct(2L, 6L, 2L, "小米14 Ultra", "徕卡光学，骁龙8 Gen3", new BigDecimal("5999"), new BigDecimal("6499"), 300, 85, 1, true),
                createProduct(3L, 6L, 3L, "iPhone 15 Pro Max", "A17 Pro芯片，钛金属设计", new BigDecimal("9999"), new BigDecimal("10999"), 200, 256, 1, true),
                createProduct(4L, 6L, 1L, "华为 nova 12", "前置6000万像素，鸿蒙4.0", new BigDecimal("2999"), new BigDecimal("3499"), 800, 42, 1, false),
                createProduct(5L, 8L, 4L, "联想 ThinkPad X1 Carbon", "商务旗舰，轻薄便携", new BigDecimal("10999"), new BigDecimal("12999"), 100, 23, 1, true),
                createProduct(6L, 8L, 3L, "MacBook Pro 14", "M3 Pro芯片，专业生产力", new BigDecimal("16999"), new BigDecimal("18999"), 80, 67, 1, true),
                createProduct(7L, 10L, 5L, "美的 对开门冰箱", "520L大容量，一级能效", new BigDecimal("4599"), new BigDecimal("5299"), 150, 38, 1, false),
                createProduct(8L, 11L, 6L, "海尔 滚筒洗衣机", "10KG大容量，直驱变频", new BigDecimal("2899"), new BigDecimal("3499"), 200, 56, 1, false),
                createProduct(9L, 7L, 1L, "华为 FreeBuds Pro 3", "旗舰降噪耳机，空间音频", new BigDecimal("1499"), new BigDecimal("1699"), 600, 189, 1, true),
                createProduct(10L, 7L, 2L, "小米 Buds 4 Pro", "智能降噪，无线充电", new BigDecimal("999"), new BigDecimal("1199"), 400, 145, 1, false),
                createProduct(11L, 13L, 7L, "耐克 Air Zoom Pegasus 40", "飞马40 跑步鞋 缓震透气", new BigDecimal("899"), new BigDecimal("1099"), 300, 120, 1, true),
                createProduct(12L, 13L, 8L, "阿迪达斯 Ultraboost Light", "UB Light 爆米花缓震跑鞋", new BigDecimal("1299"), new BigDecimal("1599"), 200, 80, 1, true),
                createProduct(13L, 12L, 12L, "海澜之家 商务休闲衬衫", "棉质长袖 正装通勤", new BigDecimal("299"), new BigDecimal("399"), 500, 200, 1, false),
                createProduct(14L, 14L, 13L, "啄木鸟 真皮手提包", "头层牛皮 通勤单肩包", new BigDecimal("599"), new BigDecimal("799"), 150, 50, 1, false),
                createProduct(15L, 12L, 14L, "安踏 运动圆领T恤", "速干透气 短袖情侣款", new BigDecimal("129"), new BigDecimal("199"), 1000, 400, 1, false),
                createProduct(16L, 6L, 9L, "荣耀 Magic6 Pro", "鹰眼长焦 骁龙8 Gen3", new BigDecimal("4999"), new BigDecimal("5499"), 400, 60, 1, true),
                createProduct(17L, 6L, 10L, "一加 12", "哈苏影像 旗舰性能", new BigDecimal("4299"), new BigDecimal("4699"), 350, 45, 1, true),
                createProduct(18L, 6L, 11L, "vivo X100 Pro", "蔡司影像 天玑9300", new BigDecimal("4599"), new BigDecimal("4999"), 300, 40, 1, false),
                createProduct(19L, 11L, 15L, "戴森 V15 Detect 吸尘器", "激光探测 智能感应", new BigDecimal("4999"), new BigDecimal("5690"), 100, 30, 1, true),
                createProduct(20L, 11L, 16L, "飞利浦 电动牙刷 HX9352", "声波震动 智能感应刷头", new BigDecimal("899"), new BigDecimal("1299"), 500, 100, 1, false)
        );
        products.forEach(productMapper::insert);

        // 创建 SKU（库存数之和 = 商品总库存）
        log.info("创建商品SKU...");
        List<ProductSku> skus = List.of(
                // P1: 华为Mate60Pro totalStock=500
                createSku(1L, 1L, "M60P-256-BLK", "雅丹黑 256GB", new BigDecimal("6999"), 200),
                createSku(2L, 1L, "M60P-256-GRN", "雅川青 256GB", new BigDecimal("6999"), 100),
                createSku(3L, 1L, "M60P-512-BLK", "雅丹黑 512GB", new BigDecimal("7999"), 150),
                createSku(4L, 1L, "M60P-512-BLU", "雅丹蓝 512GB", new BigDecimal("7999"), 50),
                // P2: 小米14Ultra totalStock=300
                createSku(5L, 2L, "X14U-256-WHT", "白色 256GB", new BigDecimal("5999"), 100),
                createSku(6L, 2L, "X14U-512-BLK", "黑色 512GB", new BigDecimal("6499"), 50),
                createSku(7L, 2L, "X14U-512-WHT", "白色 512GB", new BigDecimal("6499"), 150),
                // P3: iPhone15ProMax totalStock=200
                createSku(8L, 3L, "IP15PM-256-NAT", "原色钛金属 256GB", new BigDecimal("9999"), 50),
                createSku(9L, 3L, "IP15PM-256-BLU", "蓝色钛金属 256GB", new BigDecimal("9999"), 10),
                createSku(10L, 3L, "IP15PM-512-NAT", "原色钛金属 512GB", new BigDecimal("10999"), 100),
                createSku(11L, 3L, "IP15PM-512-WHT", "白色钛金属 512GB", new BigDecimal("10999"), 40),
                // P4: 华为nova12 totalStock=800
                createSku(12L, 4L, "NV12-256-BLU", "12号色 256GB", new BigDecimal("2999"), 500),
                createSku(13L, 4L, "NV12-256-PNK", "樱语粉 256GB", new BigDecimal("2999"), 300),
                // P5: ThinkPad X1 totalStock=100
                createSku(14L, 5L, "TP-X1-I7-16G", "i7/16GB/512GB", new BigDecimal("10999"), 60),
                createSku(15L, 5L, "TP-X1-I7-32G", "i7/32GB/1TB", new BigDecimal("12999"), 40),
                // P6: MacBook Pro 14 totalStock=80
                createSku(16L, 6L, "MBP14-M3P-18G", "M3 Pro/18GB/512GB", new BigDecimal("16999"), 45),
                createSku(17L, 6L, "MBP14-M3P-36G", "M3 Pro/36GB/1TB", new BigDecimal("18999"), 35),
                // P7: 美的冰箱 totalStock=150
                createSku(18L, 7L, "MD-RF520-SLV", "银色 520L", new BigDecimal("4599"), 150),
                // P8: 海尔洗衣机 totalStock=200
                createSku(19L, 8L, "HR-WM10-WHT", "白色 10KG", new BigDecimal("2899"), 200),
                // P9: FreeBuds Pro3 totalStock=600
                createSku(20L, 9L, "FBP3-WHT", "陶瓷白", new BigDecimal("1499"), 400),
                createSku(21L, 9L, "FBP3-BLK", "碳晶黑", new BigDecimal("1499"), 200),
                // P10: 小米Buds4Pro totalStock=400
                createSku(22L, 10L, "XB4P-WHT", "白色", new BigDecimal("999"), 250),
                createSku(23L, 10L, "XB4P-BLK", "黑色", new BigDecimal("999"), 150),
                // P11: 耐克飞马40 totalStock=300
                createSku(24L, 11L, "ZOOM-40-BLK-42", "黑色 42码", new BigDecimal("899"), 100),
                createSku(25L, 11L, "ZOOM-40-WHT-42", "白色 42码", new BigDecimal("899"), 100),
                createSku(26L, 11L, "ZOOM-40-BLK-44", "黑色 44码", new BigDecimal("999"), 50),
                createSku(27L, 11L, "ZOOM-40-WHT-44", "白色 44码", new BigDecimal("999"), 50),
                // P12: 阿迪UB Light totalStock=200
                createSku(28L, 12L, "UB-LIGHT-BLK-42", "黑色 42码", new BigDecimal("1299"), 60),
                createSku(29L, 12L, "UB-LIGHT-WHT-42", "白色 42码", new BigDecimal("1299"), 60),
                createSku(30L, 12L, "UB-LIGHT-BLK-44", "黑色 44码", new BigDecimal("1399"), 40),
                createSku(31L, 12L, "UB-LIGHT-WHT-44", "白色 44码", new BigDecimal("1399"), 40),
                // P13: 海澜之家衬衫 totalStock=500
                createSku(32L, 13L, "HLA-SHT-WHT-M", "白色 M码", new BigDecimal("299"), 200),
                createSku(33L, 13L, "HLA-SHT-BLU-L", "蓝色 L码", new BigDecimal("299"), 200),
                createSku(34L, 13L, "HLA-SHT-BLK-XL", "黑色 XL码", new BigDecimal("359"), 100),
                // P14: 啄木鸟手提包 totalStock=150
                createSku(35L, 14L, "TUC-BAG-BRW", "棕色", new BigDecimal("599"), 50),
                createSku(36L, 14L, "TUC-BAG-BLK", "黑色", new BigDecimal("599"), 50),
                createSku(37L, 14L, "TUC-BAG-RED", "红色", new BigDecimal("699"), 50),
                // P15: 安踏T恤 totalStock=1000
                createSku(38L, 15L, "ATA-T恤-WHT-M", "白色 M码", new BigDecimal("129"), 400),
                createSku(39L, 15L, "ATA-T恤-BLK-L", "黑色 L码", new BigDecimal("129"), 400),
                createSku(40L, 15L, "ATA-T恤-BLU-XL", "蓝色 XL码", new BigDecimal("159"), 200),
                // P16: 荣耀Magic6Pro totalStock=400
                createSku(41L, 16L, "MG6P-256-BLK", "黑色 256GB", new BigDecimal("4999"), 150),
                createSku(42L, 16L, "MG6P-512-BLU", "蓝色 512GB", new BigDecimal("5499"), 150),
                createSku(43L, 16L, "MG6P-256-WHT", "白色 256GB", new BigDecimal("4999"), 100),
                // P17: 一加12 totalStock=350
                createSku(44L, 17L, "ONE12-256-BLK", "黑色 256GB", new BigDecimal("4299"), 150),
                createSku(45L, 17L, "ONE12-512-BLU", "蓝色 512GB", new BigDecimal("4699"), 120),
                createSku(46L, 17L, "ONE12-256-WHT", "白色 256GB", new BigDecimal("4299"), 80),
                // P18: vivo X100Pro totalStock=300
                createSku(47L, 18L, "VX100P-256-BLK", "黑色 256GB", new BigDecimal("4599"), 100),
                createSku(48L, 18L, "VX100P-512-BLU", "蓝色 512GB", new BigDecimal("4999"), 120),
                createSku(49L, 18L, "VX100P-256-WHT", "白色 256GB", new BigDecimal("4599"), 80),
                // P19: 戴森V15 totalStock=100
                createSku(50L, 19L, "DY-V15-GLD", "金色", new BigDecimal("4999"), 40),
                createSku(51L, 19L, "DY-V15-BLK", "黑色", new BigDecimal("4999"), 30),
                createSku(52L, 19L, "DY-V15-WHT", "白色", new BigDecimal("4999"), 30),
                // P20: 飞利浦牙刷 totalStock=500
                createSku(53L, 20L, "PHILIP-WHT", "白色", new BigDecimal("899"), 250),
                createSku(54L, 20L, "PHILIP-BLK", "黑色", new BigDecimal("899"), 250)
        );
        skus.forEach(skuMapper::insert);
    }

    private Product createProduct(Long id, Long categoryId, Long brandId, String name, String subtitle,
                                  BigDecimal price, BigDecimal originalPrice, Integer stock, Integer sales, Integer status, boolean recommend) {
        Product p = new Product();
        p.setId(id);
        p.setCategoryId(categoryId);
        p.setBrandId(brandId);
        p.setName(name);
        p.setSubtitle(subtitle);
        p.setMainImage("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=" + name.replaceAll(" ", "%20") + "&image_size=square");
        p.setPrice(price);
        p.setOriginalPrice(originalPrice);
        p.setTotalStock(stock);
        p.setSales(sales);
        p.setStatus(status);
        p.setIsRecommend(recommend ? 1 : 0);
        p.setDescription("<p>" + name + " 商品详情</p>");
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        p.setDeleted(0);
        return p;
    }

    private ProductSku createSku(Long id, Long productId, String skuCode, String specInfo, BigDecimal price, Integer stock) {
        ProductSku sku = new ProductSku();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setSkuCode(skuCode);
        sku.setSpecInfo(specInfo);
        sku.setPrice(price);
        sku.setStock(stock);
        sku.setCreateTime(LocalDateTime.now());
        sku.setUpdateTime(LocalDateTime.now());
        sku.setDeleted(0);
        return sku;
    }

    private void createUsers() {
        log.info("创建用户数据...");
        String encodedPwd = passwordUtil.encode("123456");
        LocalDateTime now = LocalDateTime.now();
        List<User> users = List.of(
                createUser(2L, "zhangsan", "13800138001", "zhangsan@example.com", encodedPwd, "张三", 1, now.minusDays(7)),
                createUser(3L, "lisi", "13800138002", "lisi@example.com", encodedPwd, "李四", 1, now.minusDays(6)),
                createUser(4L, "wangwu", "13800138003", "wangwu@example.com", encodedPwd, "王五", 1, now.minusDays(5)),
                createUser(5L, "zhaoliu", "13800138004", "zhaoliu@example.com", encodedPwd, "赵六", 1, now.minusDays(4)),
                createUser(6L, "sunqi", "13800138005", "sunqi@example.com", encodedPwd, "孙七", 2, now.minusDays(3)),
                createUser(7L, "zhouba", "13800138006", "zhouba@example.com", encodedPwd, "周八", 1, now.minusDays(3)),
                createUser(8L, "wujiu", "13800138007", "wujiu@example.com", encodedPwd, "吴九", 1, now.minusDays(2)),
                createUser(9L, "zhengshi", "13800138008", "zhengshi@example.com", encodedPwd, "郑十", 2, now.minusDays(2)),
                createUser(10L, "buyer01", "13800138009", "buyer01@example.com", encodedPwd, "买家01", 1, now.minusDays(1)),
                createUser(11L, "buyer02", "13800138010", "buyer02@example.com", encodedPwd, "买家02", 2, now.minusDays(1)),
                createUser(12L, "buyer03", "13800138011", "buyer03@example.com", encodedPwd, "买家03", 1, now)
        );
        users.forEach(userMapper::insert);
    }

    private User createUser(Long id, String username, String phone, String email, String password, String nickname, Integer gender, LocalDateTime createTime) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPhone(phone);
        u.setEmail(email);
        u.setPassword(password);
        u.setSalt("");
        u.setNickname(nickname);
        u.setGender(gender);
        u.setStatus(1);
        u.setCreateTime(createTime);
        u.setUpdateTime(createTime);
        u.setDeleted(0);
        return u;
    }

    private void createAddresses() {
        log.info("创建收货地址...");
        List<Address> addresses = List.of(
                createAddress(1L, 2L, "张三", "13800138001", "北京市", "北京市", "朝阳区", "建国路88号SOHO现代城A座1501", 1),
                createAddress(2L, 3L, "李四", "13800138002", "上海市", "上海市", "浦东新区", "陆家嘴环路1000号恒生银行大厦", 1),
                createAddress(3L, 4L, "王五", "13800138003", "广东省", "深圳市", "南山区", "科技园南区深南大道10000号", 1),
                createAddress(4L, 5L, "赵六", "13800138004", "浙江省", "杭州市", "西湖区", "文三路398号东信大厦", 1),
                createAddress(5L, 6L, "孙七", "13800138005", "江苏省", "南京市", "鼓楼区", "汉中路2号", 1),
                createAddress(6L, 7L, "周八", "13800138006", "四川省", "成都市", "高新区", "天府大道北段1700号", 1),
                createAddress(7L, 8L, "吴九", "13800138007", "湖北省", "武汉市", "武昌区", "中南路1号", 1),
                createAddress(8L, 9L, "郑十", "13800138008", "陕西省", "西安市", "雁塔区", "小寨东路1号", 1),
                createAddress(9L, 10L, "买家01", "13800138009", "北京市", "北京市", "海淀区", "中关村大街1号", 1),
                createAddress(10L, 11L, "买家02", "13800138010", "上海市", "上海市", "徐汇区", "漕溪北路88号", 1),
                createAddress(11L, 12L, "买家03", "13800138011", "广东省", "广州市", "天河区", "天河路385号", 1)
        );
        addresses.forEach(addressMapper::insert);
    }

    private Address createAddress(Long id, Long userId, String name, String phone, String province, String city, String district, String detail, Integer isDefault) {
        Address a = new Address();
        a.setId(id);
        a.setUserId(userId);
        a.setReceiverName(name);
        a.setReceiverPhone(phone);
        a.setProvince(province);
        a.setCity(city);
        a.setDistrict(district);
        a.setDetailAddress(detail);
        a.setIsDefault(isDefault);
        a.setCreateTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        a.setDeleted(0);
        return a;
    }

    private void createOrders() {
        log.info("创建订单数据...");
        LocalDateTime now = LocalDateTime.now();

        // 订单数据：分布在最近7天
        List<OrderData> orderDataList = List.of(
                // 7天前 (已完成)
                new OrderData(1L, "ORD" + now.minusDays(7).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "001", 2L, new BigDecimal("6999"), 3, now.minusDays(7).withHour(10), 1L, 1L),
                // 6天前 (2单)
                new OrderData(2L, "ORD" + now.minusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "001", 3L, new BigDecimal("5999"), 3, now.minusDays(6).withHour(14), 2L, 4L),
                new OrderData(3L, "ORD" + now.minusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "002", 4L, new BigDecimal("9999"), 3, now.minusDays(6).withHour(16), 3L, 7L),
                // 5天前 (2单)
                new OrderData(4L, "ORD" + now.minusDays(5).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "001", 5L, new BigDecimal("2899"), 3, now.minusDays(5).withHour(9), 4L, 18L),
                new OrderData(5L, "ORD" + now.minusDays(5).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "002", 6L, new BigDecimal("10999"), 3, now.minusDays(5).withHour(11), 5L, 13L),
                // 4天前 (2单)
                new OrderData(6L, "ORD" + now.minusDays(4).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "001", 7L, new BigDecimal("1499"), 3, now.minusDays(4).withHour(10), 6L, 19L),
                new OrderData(7L, "ORD" + now.minusDays(4).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "002", 8L, new BigDecimal("4599"), 3, now.minusDays(4).withHour(15), 7L, 17L),
                // 3天前 (2单)
                new OrderData(8L, "ORD" + now.minusDays(3).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "001", 9L, new BigDecimal("16999"), 3, now.minusDays(3).withHour(10), 8L, 15L),
                new OrderData(9L, "ORD" + now.minusDays(3).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "002", 10L, new BigDecimal("6999"), 3, now.minusDays(3).withHour(14), 9L, 2L),
                // 2天前 (3单: 已完成2 + 已发货1)
                new OrderData(10L, "ORD" + now.minusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "001", 11L, new BigDecimal("999"), 3, now.minusDays(2).withHour(9), 10L, 21L),
                new OrderData(11L, "ORD" + now.minusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "002", 12L, new BigDecimal("2999"), 2, now.minusDays(2).withHour(11), 11L, 11L),
                new OrderData(12L, "ORD" + now.minusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "003", 2L, new BigDecimal("5999"), 2, now.minusDays(2).withHour(16), 1L, 5L),
                // 昨天 (3单: 已支付2 + 待支付1)
                new OrderData(13L, "ORD" + now.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "001", 3L, new BigDecimal("6499"), 1, now.minusDays(1).withHour(10), 2L, 6L),
                new OrderData(14L, "ORD" + now.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "002", 4L, new BigDecimal("10999"), 1, now.minusDays(1).withHour(14), 3L, 14L),
                new OrderData(15L, "ORD" + now.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "003", 5L, new BigDecimal("1499"), 0, now.minusDays(1).withHour(15), 4L, 22L),
                // 今天 (2单: 待支付 + 已支付)
                new OrderData(16L, "ORD" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "001", 2L, new BigDecimal("899"), 1, now.withHour(9), 1L, 25L),
                new OrderData(17L, "ORD" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "002", 3L, new BigDecimal("1299"), 1, now.withHour(11), 2L, 28L),
                new OrderData(18L, "ORD" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "003", 6L, new BigDecimal("4999"), 1, now.withHour(14), 6L, 41L)
        );

        for (OrderData od : orderDataList) {
            Order order = new Order();
            order.setId(od.id);
            order.setOrderNo(od.orderNo);
            order.setUserId(od.userId);
            order.setTotalAmount(od.amount);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setFreightAmount(BigDecimal.ZERO);
            order.setPayAmount(od.amount);
            order.setPayStatus(od.status >= 1 ? 1 : 0);
            order.setPayTime(od.status >= 1 ? od.createTime.plusMinutes(5) : null);
            order.setOrderStatus(od.status);
            order.setShipTime(od.status >= 2 ? od.createTime.plusHours(24) : null);
            order.setReceiveTime(od.status >= 3 ? od.createTime.plusHours(72) : null);
            order.setAddressId(od.addressId);
            order.setAddressSnapshot(toJson(od.addressId));
            if (od.status == 0) {
                order.setExpireTime(od.createTime.plusMinutes(30));
            }
            order.setCreateTime(od.createTime);
            order.setUpdateTime(od.createTime);
            order.setDeleted(0);
            orderMapper.insert(order);

            // 订单项
            OrderItem item = new OrderItem();
            item.setOrderId(od.id);
            item.setProductId(getProductIdBySkuId(od.skuId));
            item.setSkuId(od.skuId);
            item.setProductName(getProductNameBySkuId(od.skuId));
            item.setSkuInfo(getSkuSpecInfo(od.skuId));
            item.setPrice(od.amount);
            item.setQuantity(1);
            item.setSubtotal(od.amount);
            item.setCreateTime(od.createTime);
            orderItemMapper.insert(item);
        }
    }

    private String getProductNameBySkuId(Long skuId) {
        return switch (skuId.intValue()) {
            case 1,2,3,4 -> "华为 Mate 60 Pro";
            case 5,6,7 -> "小米14 Ultra";
            case 8,9,10,11 -> "iPhone 15 Pro Max";
            case 12,13 -> "华为 nova 12";
            case 14,15 -> "联想 ThinkPad X1 Carbon";
            case 16,17 -> "MacBook Pro 14";
            case 18 -> "美的 对开门冰箱";
            case 19 -> "海尔 滚筒洗衣机";
            case 20,21 -> "华为 FreeBuds Pro 3";
            case 22,23 -> "小米 Buds 4 Pro";
            case 24,25,26,27 -> "耐克 Air Zoom Pegasus 40";
            case 28,29,30,31 -> "阿迪达斯 Ultraboost Light";
            case 32,33,34 -> "海澜之家 商务休闲衬衫";
            case 35,36,37 -> "啄木鸟 真皮手提包";
            case 38,39,40 -> "安踏 运动圆领T恤";
            case 41,42,43 -> "荣耀 Magic6 Pro";
            case 44,45,46 -> "一加 12";
            case 47,48,49 -> "vivo X100 Pro";
            case 50,51,52 -> "戴森 V15 Detect 吸尘器";
            case 53,54 -> "飞利浦 电动牙刷 HX9352";
            default -> "未知商品";
        };
    }

    private String getSkuSpecInfo(Long skuId) {
        return switch (skuId.intValue()) {
            case 1 -> "雅丹黑 256GB";
            case 2 -> "雅川青 256GB";
            case 3 -> "雅丹黑 512GB";
            case 4 -> "雅丹蓝 512GB";
            case 5 -> "白色 256GB";
            case 6 -> "黑色 512GB";
            case 7 -> "白色 512GB";
            case 8 -> "原色钛金属 256GB";
            case 9 -> "蓝色钛金属 256GB";
            case 10 -> "原色钛金属 512GB";
            case 11 -> "白色钛金属 512GB";
            case 12 -> "12号色 256GB";
            case 13 -> "樱语粉 256GB";
            case 14 -> "i7/16GB/512GB";
            case 15 -> "i7/32GB/1TB";
            case 16 -> "M3 Pro/18GB/512GB";
            case 17 -> "M3 Pro/36GB/1TB";
            case 18 -> "银色 520L";
            case 19 -> "白色 10KG";
            case 20 -> "陶瓷白";
            case 21 -> "碳晶黑";
            case 22 -> "白色";
            case 23 -> "黑色";
            case 24 -> "黑色 42码";
            case 25 -> "白色 42码";
            case 26 -> "黑色 44码";
            case 27 -> "白色 44码";
            case 28 -> "黑色 42码";
            case 29 -> "白色 42码";
            case 30 -> "黑色 44码";
            case 31 -> "白色 44码";
            case 32 -> "白色 M码";
            case 33 -> "蓝色 L码";
            case 34 -> "黑色 XL码";
            case 35 -> "棕色";
            case 36 -> "黑色";
            case 37 -> "红色";
            case 38 -> "白色 M码";
            case 39 -> "黑色 L码";
            case 40 -> "蓝色 XL码";
            case 41 -> "黑色 256GB";
            case 42 -> "蓝色 512GB";
            case 43 -> "白色 256GB";
            case 44 -> "黑色 256GB";
            case 45 -> "蓝色 512GB";
            case 46 -> "白色 256GB";
            case 47 -> "黑色 256GB";
            case 48 -> "蓝色 512GB";
            case 49 -> "白色 256GB";
            case 50 -> "金色";
            case 51 -> "黑色";
            case 52 -> "白色";
            case 53 -> "白色";
            case 54 -> "黑色";
            default -> "默认规格";
        };
    }

    private Long getProductIdBySkuId(Long skuId) {
        if (skuId <= 4) return 1L;
        if (skuId <= 7) return 2L;
        if (skuId <= 11) return 3L;
        if (skuId <= 13) return 4L;
        if (skuId <= 15) return 5L;
        if (skuId <= 17) return 6L;
        if (skuId == 18) return 7L;
        if (skuId == 19) return 8L;
        if (skuId <= 21) return 9L;
        if (skuId <= 23) return 10L;
        if (skuId <= 27) return 11L;
        if (skuId <= 31) return 12L;
        if (skuId <= 34) return 13L;
        if (skuId <= 37) return 14L;
        if (skuId <= 40) return 15L;
        if (skuId <= 43) return 16L;
        if (skuId <= 46) return 17L;
        if (skuId <= 49) return 18L;
        if (skuId <= 52) return 19L;
        return 20L;
    }

    private String toJson(Long addressId) {
        Map<String, String> map = new HashMap<>();
        map.put("receiverName", "收货人" + addressId);
        map.put("receiverPhone", "1380013800" + addressId);
        map.put("province", "省" + addressId);
        map.put("city", "市" + addressId);
        map.put("district", "区" + addressId);
        map.put("detailAddress", "详细地址" + addressId);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void createBanners() {
        log.info("创建Banner数据...");
        List<Banner> banners = List.of(
                createBanner(2L, "荣耀 Magic6 Pro 新品上市", 2),
                createBanner(3L, "iPhone 15 Pro 系列特惠", 3),
                createBanner(4L, "耐克 飞马40 爆款直降", 4),
                createBanner(5L, "戴森 V15 科技新生活", 5),
                createBanner(6L, "全场9折券 限时领取", 6)
        );
        banners.forEach(bannerMapper::insert);
    }

    private Banner createBanner(Long id, String title, Integer sort) {
        Banner b = new Banner();
        b.setId(id);
        b.setTitle(title);
        b.setImageUrl("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=" + title.replaceAll(" ", "%20") + "&image_size=landscape_16_9");
        b.setSort(sort);
        b.setStatus(1);
        b.setStartTime(LocalDateTime.now().minusMonths(6));
        b.setEndTime(LocalDateTime.now().plusMonths(6));
        b.setCreateTime(LocalDateTime.now());
        b.setUpdateTime(LocalDateTime.now());
        b.setDeleted(0);
        return b;
    }

    private void createCoupons() {
        log.info("创建优惠券数据...");
        List<Coupon> coupons = List.of(
                createCoupon(1L, "新人专享券", 1, new BigDecimal("50"), new BigDecimal("299"), 1000, 850),
                createCoupon(2L, "满500减30", 1, new BigDecimal("30"), new BigDecimal("500"), 500, 420),
                createCoupon(3L, "满1000减100", 1, new BigDecimal("100"), new BigDecimal("1000"), 300, 280),
                createCoupon(4L, "数码专享券", 1, new BigDecimal("200"), new BigDecimal("3000"), 200, 150),
                createCoupon(5L, "家电优惠券", 1, new BigDecimal("150"), new BigDecimal("2000"), 150, 120),
                createCoupon(6L, "全场9折券", 2, new BigDecimal("90"), new BigDecimal("0"), 300, 250),
                createCoupon(7L, "全场85折券", 2, new BigDecimal("85"), new BigDecimal("1000"), 200, 180),
                createCoupon(8L, "满200减20", 1, new BigDecimal("20"), new BigDecimal("200"), 800, 700),
                createCoupon(9L, "满2000减300", 1, new BigDecimal("300"), new BigDecimal("2000"), 100, 80),
                createCoupon(10L, "50元无门槛券", 3, new BigDecimal("50"), new BigDecimal("0"), 500, 450),
                createCoupon(11L, "服装鞋包95折券", 2, new BigDecimal("95"), new BigDecimal("300"), 400, 350)
        );
        coupons.forEach(couponMapper::insert);

        // 用户优惠券（每个用户2-3张可用券）
        List<UserCoupon> userCoupons = List.of(
                createUserCoupon(1L, 2L, 1L), createUserCoupon(2L, 2L, 6L),
                createUserCoupon(3L, 3L, 1L), createUserCoupon(4L, 3L, 7L),
                createUserCoupon(5L, 4L, 3L), createUserCoupon(6L, 4L, 8L),
                createUserCoupon(7L, 5L, 4L), createUserCoupon(8L, 5L, 10L),
                createUserCoupon(9L, 6L, 5L), createUserCoupon(10L, 6L, 11L),
                createUserCoupon(11L, 7L, 2L), createUserCoupon(12L, 7L, 9L),
                createUserCoupon(13L, 8L, 6L), createUserCoupon(14L, 8L, 8L),
                createUserCoupon(15L, 9L, 3L), createUserCoupon(16L, 10L, 1L)
        );
        userCoupons.forEach(userCouponMapper::insert);
    }

    private Coupon createCoupon(Long id, String name, Integer type, BigDecimal value, BigDecimal minAmount, Integer total, Integer remain) {
        Coupon c = new Coupon();
        c.setId(id);
        c.setName(name);
        c.setType(type);
        c.setValue(value);
        c.setMinAmount(minAmount);
        c.setTotalCount(total);
        c.setRemainCount(remain);
        c.setStartTime(LocalDateTime.now().minusMonths(6));
        c.setEndTime(LocalDateTime.now().plusMonths(6));
        c.setStatus(1);
        c.setDescription(name);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        c.setDeleted(0);
        return c;
    }

    private UserCoupon createUserCoupon(Long id, Long userId, Long couponId) {
        UserCoupon uc = new UserCoupon();
        uc.setId(id);
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(0);
        uc.setCreateTime(LocalDateTime.now().minusDays(7));
        return uc;
    }

    private void createReviews() {
        log.info("创建评价数据...");
        List<Review> reviews = List.of(
                // 华为Mate60Pro
                createReview(1L, 2L, 1L, 5, "非常好用，续航给力，信号也强！卫星通信很实用。"),
                createReview(2L, 3L, 1L, 4, "外观漂亮，性能强劲，就是屏幕有点大"),
                createReview(3L, 4L, 1L, 5, "麒麟芯片流畅，拍照效果提升明显"),
                // 小米14Ultra
                createReview(4L, 3L, 2L, 5, "徕卡拍照太棒了，画质无敌！夜景表现惊艳"),
                createReview(5L, 5L, 2L, 4, "性能强劲，但发热控制有待改进"),
                // iPhone 15 Pro Max
                createReview(6L, 4L, 3L, 5, "钛金属质感一流，A17 Pro性能怪兽"),
                createReview(7L, 6L, 3L, 4, "生态系统完善，但信号比华为稍差"),
                // 华为nova12
                createReview(8L, 7L, 4L, 5, "性价比高，前置拍照很美"),
                // ThinkPad X1
                createReview(9L, 8L, 5L, 5, "键盘手感一流，商务首选，续航持久"),
                createReview(10L, 9L, 5L, 4, "做工扎实，但屏幕色彩一般"),
                // MacBook Pro 14
                createReview(11L, 2L, 6L, 5, "M3 Pro性能爆表，开发利器！"),
                createReview(12L, 3L, 6L, 5, "屏幕素质顶级，扬声器效果震撼"),
                // 美的冰箱
                createReview(13L, 10L, 7L, 4, "容量大，保鲜效果好，就是噪音稍大"),
                // 海尔洗衣机
                createReview(14L, 5L, 8L, 5, "洗衣机很安静，洗得干净，值得推荐"),
                createReview(15L, 7L, 8L, 4, "烘干功能很实用，省了不少时间"),
                // FreeBuds Pro 3
                createReview(16L, 2L, 9L, 5, "降噪效果非常好，音质也有提升"),
                createReview(17L, 4L, 9L, 4, "佩戴舒适，续航持久，性价比高"),
                // 耐克飞马40
                createReview(18L, 3L, 11L, 5, "缓震效果出色，跑步回弹好，穿起来很轻"),
                createReview(19L, 8L, 11L, 4, "透气性好，长时间穿着不闷脚"),
                // 阿迪达斯UB Light
                createReview(20L, 5L, 12L, 5, "爆米花中底名不虚传，走路就像踩棉花"),
                // 海澜之家衬衫
                createReview(21L, 6L, 13L, 4, "面料舒适，版型修身，商务休闲都能穿"),
                // 啄木鸟手提包
                createReview(22L, 9L, 14L, 5, "真皮质感好，做工精细，容量刚刚好"),
                // 安踏T恤
                createReview(23L, 10L, 15L, 4, "速干面料很舒服，运动穿很合适"),
                // 荣耀Magic6Pro
                createReview(24L, 2L, 16L, 5, "鹰眼长焦很厉害，夜拍效果出色"),
                createReview(25L, 3L, 16L, 4, "外观设计时尚，性价比高"),
                // 一加12
                createReview(26L, 5L, 17L, 5, "哈苏影像调校不错，性能旗舰级别"),
                // vivo X100Pro
                createReview(27L, 7L, 18L, 4, "蔡司镜头素质高，系统流畅好用"),
                // 戴森V15
                createReview(28L, 4L, 19L, 5, "激光显示灰尘太直观了，吸力强劲"),
                // 飞利浦牙刷
                createReview(29L, 6L, 20L, 4, "声波震动温和，清洁效果好，续航长")
        );
        reviews.forEach(reviewMapper::insert);
    }

    private Review createReview(Long id, Long userId, Long productId, Integer rating, String content) {
        Review r = new Review();
        r.setId(id);
        r.setUserId(userId);
        r.setProductId(productId);
        r.setOrderId(id); // 关联订单ID
        r.setRating(rating);
        r.setContent(content);
        r.setStatus(1);
        r.setParentId(0L);
        r.setReplyStatus(0);
        r.setCreateTime(LocalDateTime.now().minusDays(3));
        r.setUpdateTime(LocalDateTime.now().minusDays(3));
        r.setDeleted(0);
        return r;
    }

    private static class OrderData {
        Long id;
        String orderNo;
        Long userId;
        BigDecimal amount;
        Integer status;
        LocalDateTime createTime;
        Long addressId;
        Long skuId;

        OrderData(Long id, String orderNo, Long userId, BigDecimal amount, Integer status, LocalDateTime createTime, Long addressId, Long skuId) {
            this.id = id;
            this.orderNo = orderNo;
            this.userId = userId;
            this.amount = amount;
            this.status = status;
            this.createTime = createTime;
            this.addressId = addressId;
            this.skuId = skuId;
        }
    }
}