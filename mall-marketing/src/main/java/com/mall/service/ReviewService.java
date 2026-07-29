package com.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.Review;
import java.util.Map;

public interface ReviewService {
    Page<Review> pageByProductId(Long productId, Integer current, Integer size, Integer ratingType, Boolean hasImages);
    Page<Review> pageForAdmin(Integer current, Integer size, Integer rating, Integer status);
    Map<String, Object> summaryByProductId(Long productId);
    Page<Review> pageByUserId(Long userId, Integer current, Integer size);
    Review getById(Long id);
    void save(Review review);
    void reply(Long id, String reply);
    void deleteById(Long id);
    void updateStatus(Long id, Integer status);
}
