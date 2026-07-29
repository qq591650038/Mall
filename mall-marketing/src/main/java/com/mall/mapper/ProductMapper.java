package com.mall.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.List;
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
 Page<Product> selectPublicPage(Page<Product> page, @Param("keyword") String keyword,
                               @Param("categoryId") Long categoryId, @Param("brandId") Long brandId,
                               @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice,
                               @Param("minRating") Double minRating, @Param("inStock") Boolean inStock,
                               @Param("sort") String sort);
 List<String> selectSuggestions(@Param("keyword") String keyword, @Param("limit") Integer limit);
 List<Product> selectRelated(@Param("productId") Long productId, @Param("categoryId") Long categoryId,
                             @Param("brandId") Long brandId, @Param("limit") Integer limit);
 int decrementTotalStock(Long productId, Integer quantity);
 int incrementTotalStock(Long productId, Integer quantity);
}
