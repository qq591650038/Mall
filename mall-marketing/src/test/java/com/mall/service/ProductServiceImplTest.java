package com.mall.service;

import com.mall.entity.Product;
import com.mall.entity.ProductSku;
import com.mall.mapper.*;
import com.mall.service.impl.ProductServiceImpl;
import com.mall.utils.RedisUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import com.mall.exception.BusinessException;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductServiceImplTest {
    @Test
    void saveUsesSkuUpsertForRepeatedBusinessKey() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductServiceImpl service = new ProductServiceImpl(productMapper, skuMapper,
                mock(ProductImageMapper.class), mock(CategoryMapper.class), mock(BrandMapper.class),
                mock(ReviewMapper.class), mock(BrowseHistoryMapper.class), mock(RedisUtil.class), new SimpleMeterRegistry());
        Product product = new Product();
        product.setName("测试商品");
        ProductSku sku = new ProductSku();
        sku.setSkuCode("SKU-1");
        sku.setSpecInfo("标准");
        sku.setPrice(new BigDecimal("10.00"));
        sku.setStock(5);
        product.setSkus(java.util.List.of(sku));
        service.save(product);
        verify(skuMapper).upsert(any(ProductSku.class));
    }

    @Test
    void saveRejectsDuplicateSkuCodeInOneProduct() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductServiceImpl service = new ProductServiceImpl(productMapper, skuMapper,
                mock(ProductImageMapper.class), mock(CategoryMapper.class), mock(BrandMapper.class),
                mock(ReviewMapper.class), mock(BrowseHistoryMapper.class), mock(RedisUtil.class), new SimpleMeterRegistry());

        Product product = new Product();
        ProductSku first = sku("SKU-DUP");
        ProductSku second = sku("SKU-DUP");
        product.setSkus(java.util.List.of(first, second));

        assertThrows(BusinessException.class, () -> service.save(product));
        verify(productMapper, never()).insert(any(Product.class));
        verify(skuMapper, never()).upsert(any(ProductSku.class));
    }

    @Test
    void pageFallsBackToDatabaseWhenRedisCacheMisses() {
        ProductMapper productMapper = mock(ProductMapper.class);
        RedisUtil redis = mock(RedisUtil.class);
        when(redis.get(anyString())).thenReturn(null);
        when(productMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProductServiceImpl service = new ProductServiceImpl(productMapper, mock(ProductSkuMapper.class),
                mock(ProductImageMapper.class), mock(CategoryMapper.class), mock(BrandMapper.class),
                mock(ReviewMapper.class), mock(BrowseHistoryMapper.class), redis, new SimpleMeterRegistry());

        service.page(1, 10, "手机", null, 1);

        verify(productMapper).selectPage(any(), any());
        verify(redis).set(anyString(), any(), eq(2L), any());
    }

    private ProductSku sku(String code) {
        ProductSku sku = new ProductSku();
        sku.setSkuCode(code);
        sku.setSpecInfo("标准");
        sku.setPrice(new BigDecimal("10.00"));
        sku.setStock(5);
        return sku;
    }
}
