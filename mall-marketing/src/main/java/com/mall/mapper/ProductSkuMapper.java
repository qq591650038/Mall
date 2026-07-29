package com.mall.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
    int upsert(ProductSku sku);
    int decrementStock(Long skuId, Integer quantity);
    int incrementStock(Long skuId, Integer quantity);
}
