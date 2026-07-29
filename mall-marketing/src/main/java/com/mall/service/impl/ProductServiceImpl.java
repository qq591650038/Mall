package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.entity.*;
import com.mall.exception.BusinessException;
import com.mall.mapper.*;
import com.mall.service.ProductService;
import com.mall.utils.RedisUtil;
import com.mall.vo.ProductDetailVO;
import com.mall.vo.ProductReviewSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private static final String CACHE_KEY_PREFIX = "product:detail:";
    private static final long CACHE_TTL_MINUTES = 30;
    private static final String LIST_CACHE_PREFIX = "product:list:";
    private final ConcurrentHashMap<String, ReentrantLock> cacheLocks = new ConcurrentHashMap<>();

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;
    private final ReviewMapper reviewMapper;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final RedisUtil redisUtil;
    private final MeterRegistry meterRegistry;

    public ProductServiceImpl(ProductMapper productMapper,
                              ProductSkuMapper productSkuMapper,
                              ProductImageMapper productImageMapper,
                              CategoryMapper categoryMapper,
                              BrandMapper brandMapper,
                              ReviewMapper reviewMapper,
                              BrowseHistoryMapper browseHistoryMapper,
                              RedisUtil redisUtil, MeterRegistry meterRegistry) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.productImageMapper = productImageMapper;
        this.categoryMapper = categoryMapper;
        this.brandMapper = brandMapper;
        this.reviewMapper = reviewMapper;
        this.browseHistoryMapper = browseHistoryMapper;
        this.redisUtil = redisUtil;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Page<Product> page(Integer current, Integer size, String keyword, Long categoryId, Integer status) {
        String key = LIST_CACHE_PREFIX + current + ":" + size + ":" + (keyword == null ? "" : keyword) + ":" + categoryId + ":" + status;
        Object cached = redisUtil.get(key);
        if ("__EMPTY__".equals(cached)) return new Page<>(current, size);
        if (cached instanceof Page) return enrichReviewSummaries((Page<Product>) cached);
        ReentrantLock lock = cacheLocks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            cached = redisUtil.get(key);
            if ("__EMPTY__".equals(cached)) return new Page<>(current, size);
            if (cached instanceof Page) return enrichReviewSummaries((Page<Product>) cached);
            Page<Product> page = new Page<>(current, size);
            QueryWrapper<Product> wrapper = new QueryWrapper<>();
            if (StringUtils.hasText(keyword)) {
                wrapper.apply("MATCH(name, subtitle, description) AGAINST ({0} IN BOOLEAN MODE)", keyword + "*");
            }
            if (categoryId != null) {
                wrapper.eq("category_id", categoryId);
            }
            if (status != null) {
                wrapper.eq("status", status);
            }
            wrapper.orderByDesc("id");
            Page<Product> result = productMapper.selectPage(page, wrapper);
            redisUtil.set(key, result.getRecords().isEmpty() ? "__EMPTY__" : result, 2, TimeUnit.MINUTES);
            return enrichReviewSummaries(result);
        } finally {
            lock.unlock();
            cacheLocks.remove(key, lock);
        }
    }

    @Override
    public Page<Product> publicPage(Integer current, Integer size, String keyword, Long categoryId, Long brandId,
                                    BigDecimal minPrice, BigDecimal maxPrice, Double minRating, Boolean inStock, String sort) {
        Page<Product> page = productMapper.selectPublicPage(new Page<>(current, size), keyword, categoryId, brandId,
                minPrice, maxPrice, minRating, inStock, normalizeSort(sort));
        // 填充评价摘要数据（评论数和平均评分）
        return enrichReviewSummaries(page);
    }

    private String normalizeSort(String sort) {
        return Set.of("sales", "priceAsc", "priceDesc", "rating", "newest").contains(sort) ? sort : "default";
    }

    @Override
    public List<String> suggestions(String keyword, Integer limit) {
        if (!StringUtils.hasText(keyword)) return List.of();
        return productMapper.selectSuggestions(keyword.trim(), Math.min(limit == null ? 8 : limit, 10));
    }

    @Override
    public List<String> popularSearches(Integer limit) {
        return productMapper.selectList(new QueryWrapper<Product>().eq("status", 1).orderByDesc("sales")
                        .last("LIMIT " + Math.min(limit == null ? 8 : limit, 10))).stream()
                .map(Product::getName).toList();
    }

    @Override
    public List<Product> related(Long productId, Integer limit) {
        Product product = productMapper.selectById(productId);
        if (product == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST);
        return productMapper.selectRelated(productId, product.getCategoryId(), product.getBrandId(), Math.min(limit == null ? 5 : limit, 10));
    }

    @Override
    public List<Product> recommendations(Long userId, Integer limit) {
        int safeLimit = Math.min(limit == null ? 10 : limit, 20);
        Long categoryId = null;
        if (userId != null) {
            BrowseHistory latest = browseHistoryMapper.selectOne(new QueryWrapper<BrowseHistory>()
                    .eq("user_id", userId).orderByDesc("browse_time").last("LIMIT 1"));
            if (latest != null) {
                Product viewed = productMapper.selectById(latest.getProductId());
                if (viewed != null) categoryId = viewed.getCategoryId();
            }
        }
        Page<Product> page = publicPage(1, safeLimit, null, categoryId, null, null, null, 4D, true, "rating");
        if (page.getRecords().size() < safeLimit) {
            Page<Product> fallback = publicPage(1, safeLimit, null, null, null, null, null, null, true, "sales");
            Map<Long, Product> unique = new java.util.LinkedHashMap<>();
            page.getRecords().forEach(p -> unique.put(p.getId(), p));
            fallback.getRecords().forEach(p -> unique.putIfAbsent(p.getId(), p));
            return new ArrayList<>(unique.values()).stream().limit(safeLimit).toList();
        }
        return page.getRecords();
    }

    private Page<Product> enrichReviewSummaries(Page<Product> page) {
        if (page.getRecords().isEmpty()) return page;
        List<Long> productIds = page.getRecords().stream().map(Product::getId).toList();
        Map<Long, ProductReviewSummaryVO> summaries = reviewMapper.selectProductSummaries(productIds).stream()
                .collect(Collectors.toMap(ProductReviewSummaryVO::getProductId, summary -> summary));
        page.getRecords().forEach(product -> {
            ProductReviewSummaryVO summary = summaries.get(product.getId());
            product.setReviewCount(summary == null ? 0L : summary.getReviewCount());
            product.setAverageRating(summary == null ? 0D : summary.getAverageRating());
        });
        return page;
    }

    @Override
    public ProductDetailVO getDetail(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof ProductDetailVO) {
            meterRegistry.counter("mall.cache.requests", "cache", "product_detail", "result", "hit").increment();
            log.debug("命中商品缓存: productId={}", id);
            ProductDetailVO cachedVo = (ProductDetailVO) cached;
            refreshRealtimeData(cachedVo);
            return cachedVo;
        }

        Product product = productMapper.selectById(id);
        meterRegistry.counter("mall.cache.requests", "cache", "product_detail", "result", "miss").increment();
        if (product == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST);
        ProductDetailVO vo = buildDetailVO(product);
        redisUtil.set(cacheKey, vo, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return vo;
    }

    private void refreshRealtimeData(ProductDetailVO vo) {
        Product product = productMapper.selectById(vo.getId());
        if (product == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST);
        refreshRealtimeData(vo, product);
    }

    private ProductDetailVO buildDetailVO(Product product) {
        ProductDetailVO vo = convertToDetailVO(product);

        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        if (product.getBrandId() != null) {
            Brand brand = brandMapper.selectById(product.getBrandId());
            if (brand != null) {
                vo.setBrandName(brand.getName());
            }
        }

        List<ProductImage> images = productImageMapper.selectList(
                new QueryWrapper<ProductImage>().eq("product_id", product.getId()).orderByAsc("sort")
        );
        vo.setImages(images.stream().map(ProductImage::getUrl).collect(Collectors.toList()));

        List<ProductSku> skus = productSkuMapper.selectList(
                new QueryWrapper<ProductSku>().eq("product_id", product.getId())
        );
        vo.setSkus(skus.stream().map(this::convertToSkuVO).collect(Collectors.toList()));

        return vo;
    }

    private void refreshRealtimeData(ProductDetailVO vo, Product product) {
        vo.setTotalStock(product.getTotalStock());
        vo.setStatus(product.getStatus());
        vo.setSales(product.getSales());

        if (vo.getSkus() == null || vo.getSkus().isEmpty()) {
            return;
        }

        List<Long> skuIds = vo.getSkus().stream().map(ProductDetailVO.SkuVO::getId).collect(Collectors.toList());
        Map<Long, ProductSku> skuMap = productSkuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));

        for (ProductDetailVO.SkuVO skuVO : vo.getSkus()) {
            ProductSku sku = skuMap.get(skuVO.getId());
            if (sku != null) {
                skuVO.setStock(sku.getStock());
            }
        }
    }

    private void evictCache(Long productId) {
        redisUtil.delete(CACHE_KEY_PREFIX + productId);
        log.debug("清除商品缓存: productId={}", productId);
    }

    @Override
    public Product getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST);
        }
        product.setSkus(productSkuMapper.selectList(new QueryWrapper<ProductSku>().eq("product_id", id).orderByAsc("id")));
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Product product) {
        if (product.getSkus() != null) {
            normalizeSkuCodes(product.getSkus());
            validateSkus(product.getSkus());
        }
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        product.setDeleted(0);
        product.setStatus(0);
        productMapper.insert(product);
        saveSkus(product);
        log.info("新增商品: {}", product.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Product product) {
        if (product.getSkus() != null) {
            normalizeSkuCodes(product.getSkus());
            validateSkus(product.getSkus());
        }
        Product exist = productMapper.selectById(product.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST);
        }
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        syncSkus(product);
        evictCache(product.getId());
        log.info("更新商品: id={}", product.getId());
    }

    private void saveSkus(Product product) {
        if (product.getSkus() == null) return;
        normalizeSkuCodes(product.getSkus());
        validateSkus(product.getSkus());
        int total = 0;
        for (ProductSku sku : product.getSkus()) {
            sku.setProductId(product.getId());
            sku.setStatus(sku.getStatus() == null ? 1 : sku.getStatus());
            sku.setCreateTime(LocalDateTime.now());
            sku.setUpdateTime(LocalDateTime.now());
            sku.setDeleted(0);
            productSkuMapper.upsert(sku);
            total += sku.getStock() == null ? 0 : sku.getStock();
        }
        product.setTotalStock(total);
        productMapper.updateById(product);
    }

    private void syncSkus(Product product) {
        if (product.getSkus() == null) return;
        validateSkus(product.getSkus());
        productSkuMapper.delete(new QueryWrapper<ProductSku>().eq("product_id", product.getId()));
        saveSkus(product);
    }

    private void validateSkus(List<ProductSku> skus) {
        Set<String> codes = new HashSet<>();
        for (ProductSku sku : skus) {
            if (sku.getPrice() == null || sku.getPrice().signum() < 0 || sku.getStock() == null || sku.getStock() < 0 || sku.getSpecInfo() == null || sku.getSpecInfo().isBlank())
                throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU 规格、价格和库存不能为空且不能为负数");
            String code = sku.getSkuCode() == null ? "" : sku.getSkuCode().trim();
            if (!code.isEmpty() && !codes.add(code))
                throw new BusinessException(ErrorCode.CONFLICT, "SKU 编码不能重复");
        }
    }

    private void normalizeSkuCodes(List<ProductSku> skus) {
        for (ProductSku sku : skus) {
            if (sku.getSkuCode() == null || sku.getSkuCode().isBlank()) {
                sku.setSkuCode("SKU-" + cn.hutool.core.util.IdUtil.fastSimpleUUID().substring(0, 16).toUpperCase());
            } else {
                sku.setSkuCode(sku.getSkuCode().trim());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST);
        }
        productSkuMapper.delete(
                new QueryWrapper<ProductSku>().eq("product_id", id)
        );
        productImageMapper.delete(
                new QueryWrapper<ProductImage>().eq("product_id", id)
        );
        productMapper.deleteById(id);
        evictCache(id);
        log.info("删除商品: id={}", id);
    }

    @Override
    public void onShelf(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST);
        }
        if (product.getTotalStock() <= 0) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存为0，无法上架");
        }
        product.setStatus(1);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        evictCache(id);
        log.info("商品上架: id={}", id);
    }

    @Override
    public void offShelf(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST);
        }
        product.setStatus(0);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        evictCache(id);
        log.info("商品下架: id={}", id);
    }

    private ProductDetailVO convertToDetailVO(Product product) {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setMainImage(product.getMainImage());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setTotalStock(product.getTotalStock());
        vo.setSales(product.getSales());
        vo.setStatus(product.getStatus());
        vo.setIsRecommend(product.getIsRecommend());
        vo.setDescription(product.getDescription());
        vo.setCategoryId(product.getCategoryId());
        vo.setBrandId(product.getBrandId());
        return vo;
    }

    private ProductDetailVO.SkuVO convertToSkuVO(ProductSku sku) {
        ProductDetailVO.SkuVO vo = new ProductDetailVO.SkuVO();
        vo.setId(sku.getId());
        vo.setSkuCode(sku.getSkuCode());
        vo.setSpecInfo(sku.getSpecInfo());
        vo.setPrice(sku.getPrice());
        vo.setStock(sku.getStock());
        vo.setImage(sku.getImage());
        return vo;
    }
}
