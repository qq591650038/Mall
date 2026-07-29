package com.mall.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.PointsProduct;
import com.mall.entity.PointsRedemption;
import com.mall.exception.BusinessException;
import com.mall.mapper.PointsProductMapper;
import com.mall.mapper.PointsRedemptionMapper;
import com.mall.service.PointsRewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员积分兑换商品管理控制器
 * 提供积分兑换商品的增删改查和兑换记录查询
 */
@RestController
@RequestMapping("/api/admin/points-products")
@Tag(name = "积分兑换管理", description = "管理员积分兑换商品和记录管理")
public class AdminPointsProductController {

    private final PointsProductMapper productMapper;
    private final PointsRedemptionMapper redemptionMapper;
    private final PointsRewardService pointsRewardService;

    public AdminPointsProductController(PointsProductMapper productMapper,
                                         PointsRedemptionMapper redemptionMapper,
                                         PointsRewardService pointsRewardService) {
        this.productMapper = productMapper;
        this.redemptionMapper = redemptionMapper;
        this.pointsRewardService = pointsRewardService;
    }

    /**
     * 分页查询积分兑换商品
     */
    @GetMapping("/page")
    @Operation(summary = "兑换商品分页", description = "获取积分兑换商品分页列表")
    public Result<PageResult<PointsProduct>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        Page<PointsProduct> page = new Page<>(current, size);
        QueryWrapper<PointsProduct> wrapper = new QueryWrapper<>();

        if (status != null) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("name", keyword);
        }
        wrapper.orderByDesc("id");

        Page<PointsProduct> productPage = productMapper.selectPage(page, wrapper);
        PageResult<PointsProduct> result = new PageResult<>(
                productPage.getTotal(), productPage.getRecords(), current, size);
        return Result.success(result);
    }

    /**
     * 获取兑换商品详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "兑换商品详情", description = "获取单个积分兑换商品详情")
    public Result<PointsProduct> getById(@PathVariable Long id) {
        PointsProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.POINTS_PRODUCT_NOT_EXIST);
        }
        return Result.success(product);
    }

    /**
     * 新增积分兑换商品
     */
    @PostMapping
    @Operation(summary = "新增兑换商品", description = "创建新的积分兑换商品")
    public Result<PointsProduct> create(@RequestBody PointsProduct product) {
        // 校验必填字段
        if (product.getName() == null || product.getName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品名称不能为空");
        }
        if (product.getPointsCost() == null || product.getPointsCost() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "所需积分必须大于0");
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getRewardType() == null) {
            product.setRewardType("COUPON");
        }
        if (product.getStock() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "库存不能小于0");
        }
        pointsRewardService.validateConfiguration(product);

        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
        return Result.success(product);
    }

    /**
     * 更新积分兑换商品
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新兑换商品", description = "更新积分兑换商品信息")
    public Result<PointsProduct> update(@PathVariable Long id,
                                         @RequestBody PointsProduct product) {
        PointsProduct existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.POINTS_PRODUCT_NOT_EXIST);
        }

        PointsProduct merged = merge(existing, product);
        pointsRewardService.validateConfiguration(merged);
        product.setId(id);
        product.setUpdateTime(LocalDateTime.now());
        // 不允许修改已兑换记录的商品时直接删除或改为不可用，允许修改库存和积分
        productMapper.updateById(product);
        return Result.success(product);
    }

    /**
     * 删除积分兑换商品（软删除通过设置status=0实现）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除兑换商品", description = "软删除积分兑换商品（设置为下架状态）")
    public Result<Void> delete(@PathVariable Long id) {
        PointsProduct existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.POINTS_PRODUCT_NOT_EXIST);
        }
        // 软删除：设置状态为0（下架），保留历史兑换记录
        existing.setStatus(0);
        existing.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(existing);
        return Result.success("删除成功", null);
    }

    /**
     * 上架/下架兑换商品
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "切换上下架", description = "上架或下架积分兑换商品")
    public Result<Void> updateStatus(@PathVariable Long id,
                                      @RequestParam Integer status) {
        PointsProduct existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.POINTS_PRODUCT_NOT_EXIST);
        }
        existing.setStatus(status);
        if (status != null && status == 1) {
            pointsRewardService.validateConfiguration(existing);
        }
        existing.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(existing);
        return Result.success("操作成功", null);
    }

    /**
     * 查询兑换记录
     */
    @GetMapping("/redemptions")
    @Operation(summary = "兑换记录", description = "查询积分兑换记录分页列表")
    public Result<PageResult<PointsRedemption>> redemptions(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long productId) {

        Page<PointsRedemption> page = new Page<>(current, size);
        QueryWrapper<PointsRedemption> wrapper = new QueryWrapper<>();

        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        wrapper.orderByDesc("create_time");

        Page<PointsRedemption> redemptionPage = redemptionMapper.selectPage(page, wrapper);
        PageResult<PointsRedemption> result = new PageResult<>(
                redemptionPage.getTotal(), redemptionPage.getRecords(), current, size);
        return Result.success(result);
    }

    /**
     * 批量初始化兑换商品数据
     */
    @PostMapping("/init")
    @Operation(summary = "初始化兑换商品", description = "初始化默认积分兑换商品数据")
    public Result<List<PointsProduct>> initProducts() {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请通过奖励下拉框关联真实优惠券或商品");
        /*
        // 检查是否已有数据
        Long count = productMapper.selectCount(new QueryWrapper<>());
        if (count > 0) {
            return Result.success("已有兑换商品数据，跳过初始化", List.of());
        }

        // 初始化默认兑换商品
        PointsProduct product1 = new PointsProduct();
        product1.setName("满100减10元优惠券");
        product1.setDescription("购物满100元可用，有效期30天");
        product1.setPointsCost(100);
        product1.setStock(1000);
        product1.setRewardType("COUPON");
        product1.setRewardValue("COUPON_10_OFF_100");
        product1.setStatus(1);
        product1.setCreateTime(LocalDateTime.now());
        product1.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product1);

        PointsProduct product2 = new PointsProduct();
        product2.setName("满200减30元优惠券");
        product2.setDescription("购物满200元可用，有效期30天");
        product2.setPointsCost(200);
        product2.setStock(500);
        product2.setRewardType("COUPON");
        product2.setRewardValue("COUPON_30_OFF_200");
        product2.setStatus(1);
        product2.setCreateTime(LocalDateTime.now());
        product2.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product2);

        PointsProduct product3 = new PointsProduct();
        product3.setName("9折优惠券");
        product3.setDescription("全场9折，无门槛，有效期7天");
        product3.setPointsCost(150);
        product3.setStock(300);
        product3.setRewardType("COUPON");
        product3.setRewardValue("COUPON_9折");
        product3.setStatus(1);
        product3.setCreateTime(LocalDateTime.now());
        product3.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product3);

        PointsProduct product4 = new PointsProduct();
        product4.setName("品牌精选T恤");
        product4.setDescription("纯棉材质，多色可选，限量兑换");
        product4.setPointsCost(1000);
        product4.setStock(50);
        product4.setRewardType("PHYSICAL");
        product4.setRewardValue("BRAND_TSHIRT_2024");
        product4.setStatus(1);
        product4.setCreateTime(LocalDateTime.now());
        product4.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product4);

        PointsProduct product5 = new PointsProduct();
        product5.setName("蓝牙耳机");
        product5.setDescription("主动降噪，续航30小时，高品质音效");
        product5.setPointsCost(5000);
        product5.setStock(10);
        product5.setRewardType("PHYSICAL");
        product5.setRewardValue("BT_EARPHONE_PRO");
        product5.setStatus(1);
        product5.setCreateTime(LocalDateTime.now());
        product5.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product5);

        List<PointsProduct> products = productMapper.selectList(new QueryWrapper<PointsProduct>()
                .orderByDesc("id"));
        return Result.success("初始化成功", products); */
    }

    private PointsProduct merge(PointsProduct existing, PointsProduct input) {
        PointsProduct merged = new PointsProduct();
        merged.setId(existing.getId());
        merged.setName(input.getName() == null ? existing.getName() : input.getName());
        merged.setDescription(input.getDescription() == null ? existing.getDescription() : input.getDescription());
        merged.setPointsCost(input.getPointsCost() == null ? existing.getPointsCost() : input.getPointsCost());
        merged.setStock(input.getStock() == null ? existing.getStock() : input.getStock());
        merged.setRewardType(input.getRewardType() == null ? existing.getRewardType() : input.getRewardType());
        merged.setRewardRefId(input.getRewardRefId() == null ? existing.getRewardRefId() : input.getRewardRefId());
        merged.setRewardSkuId(input.getRewardSkuId() == null ? existing.getRewardSkuId() : input.getRewardSkuId());
        merged.setStatus(input.getStatus() == null ? existing.getStatus() : input.getStatus());
        if (merged.getPointsCost() == null || merged.getPointsCost() <= 0 || merged.getStock() == null || merged.getStock() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "积分必须大于0且库存不能小于0");
        }
        return merged;
    }
}
