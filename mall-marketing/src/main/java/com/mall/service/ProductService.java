package com.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.Product;
import com.mall.vo.ProductDetailVO;
import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    Page<Product> page(Integer current, Integer size, String keyword, Long categoryId, Integer status);
    Page<Product> publicPage(Integer current, Integer size, String keyword, Long categoryId, Long brandId,
                             BigDecimal minPrice, BigDecimal maxPrice, Double minRating, Boolean inStock, String sort);
    List<String> suggestions(String keyword, Integer limit);
    List<String> popularSearches(Integer limit);
    List<Product> related(Long productId, Integer limit);
    List<Product> recommendations(Long userId, Integer limit);
    ProductDetailVO getDetail(Long id);
    Product getById(Long id);
    void save(Product product);
    void update(Product product);
    void deleteById(Long id);
    void onShelf(Long id);
    void offShelf(Long id);
}
