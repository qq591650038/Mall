package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.common.result.OrderStatus;
import com.mall.entity.Order;
import com.mall.entity.OrderItem;
import com.mall.entity.Product;
import com.mall.entity.Review;
import com.mall.entity.User;
import com.mall.exception.BusinessException;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ReviewMapper;
import com.mall.mapper.UserMapper;
import com.mall.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper,
                             OrderMapper orderMapper,
                             OrderItemMapper orderItemMapper,
                             ProductMapper productMapper,
                             UserMapper userMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Page<Review> pageByProductId(Long productId, Integer current, Integer size, Integer ratingType, Boolean hasImages) {
        Page<Review> page = new Page<>(current, size);
        QueryWrapper<Review> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId)
                .eq("status", 1)
                .eq("parent_id", 0);
        if (ratingType != null) {
            if (ratingType == 1) wrapper.le("rating", 2);
            else if (ratingType == 2) wrapper.eq("rating", 3);
            else if (ratingType == 3) wrapper.ge("rating", 4);
        }
        if (Boolean.TRUE.equals(hasImages)) wrapper.isNotNull("images").ne("images", "");
        wrapper.orderByDesc("create_time");
        return reviewMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<Review> pageForAdmin(Integer current, Integer size, Integer rating, Integer status) {
        Page<Review> page = reviewMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<Review>().eq("parent_id", 0)
                        .eq(rating != null, "rating", rating)
                        .eq(status != null, "status", status)
                        .orderByDesc("create_time"));
        if (page.getRecords().isEmpty()) return page;
        var productIds = page.getRecords().stream().map(Review::getProductId).collect(Collectors.toSet());
        var userIds = page.getRecords().stream().map(Review::getUserId).collect(Collectors.toSet());
        Map<Long, Product> products = productIds.isEmpty() ? Collections.emptyMap() : productMapper.selectBatchIds(productIds)
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap() : userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        page.getRecords().forEach(review -> {
            Product product = products.get(review.getProductId());
            User user = users.get(review.getUserId());
            review.setProductName(product == null ? "商品已删除" : product.getName());
            review.setUsername(user == null ? "用户已删除" : (user.getNickname() == null ? user.getUsername() : user.getNickname()));
        });
        return page;
    }

    @Override
    public Map<String, Object> summaryByProductId(Long productId) {
        QueryWrapper<Review> base = new QueryWrapper<Review>().eq("product_id", productId)
                .eq("status", 1).eq("parent_id", 0);
        long total = reviewMapper.selectCount(base);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        if (total == 0) {
            result.put("good", 0L);
            result.put("average", 0D);
            result.put("ratingCounts", Map.of("1", 0L, "2", 0L, "3", 0L, "4", 0L, "5", 0L));
            return result;
        }
        long good = reviewMapper.selectCount(new QueryWrapper<Review>().eq("product_id", productId)
                .eq("status", 1).eq("parent_id", 0).ge("rating", 4));
        double average = reviewMapper.selectObjs(new QueryWrapper<Review>().select("AVG(rating)")
                .eq("product_id", productId).eq("status", 1).eq("parent_id", 0)).stream()
                .findFirst().map(v -> ((Number) v).doubleValue()).orElse(0D);
        result.put("good", good);
        result.put("average", Math.round(average * 10D) / 10D);
        Map<String, Long> ratingCounts = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            long count = reviewMapper.selectCount(new QueryWrapper<Review>().eq("product_id", productId)
                    .eq("status", 1).eq("parent_id", 0).eq("rating", rating));
            ratingCounts.put(String.valueOf(rating), count);
        }
        result.put("ratingCounts", ratingCounts);
        return result;
    }

    @Override
    public Page<Review> pageByUserId(Long userId, Integer current, Integer size) {
        Page<Review> page = new Page<>(current, size);
        QueryWrapper<Review> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        Page<Review> result = reviewMapper.selectPage(page, wrapper);
        result.getRecords().forEach(review -> {
            Product product = productMapper.selectById(review.getProductId());
            review.setProductName(product == null ? "商品已删除" : product.getName());
        });
        return result;
    }

    @Override
    public Review getById(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        return review;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Review review) {
        if (review.getOrderId() != null && review.getProductId() != null) {
            Order order = orderMapper.selectById(review.getOrderId());
            if (order == null || !order.getUserId().equals(review.getUserId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_EXIST);
            }
            if (OrderStatus.COMPLETED.getCode() != order.getOrderStatus()
                    && OrderStatus.SHIPPED.getCode() != order.getOrderStatus()) {
                throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "只有已发货或已完成的订单才能评价");
            }

            Long itemCount = orderItemMapper.selectCount(new QueryWrapper<OrderItem>()
                    .eq("order_id", review.getOrderId()).eq("product_id", review.getProductId()));
            if (itemCount == 0) throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST, "该商品不在订单中");

            if (review.getParentId() == null || review.getParentId() == 0) {
                Long existCount = reviewMapper.selectCount(
                        new QueryWrapper<Review>()
                                .eq("order_id", review.getOrderId())
                                .eq("product_id", review.getProductId())
                                .eq("parent_id", 0)
                );
                if (existCount > 0) {
                    throw new BusinessException(ErrorCode.CONFLICT, "该商品已评价过");
                }
            }
        }

        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        review.setDeleted(0);
        if (review.getStatus() == null) {
            review.setStatus(1);
        }
        if (review.getParentId() == null) {
            review.setParentId(0L);
        }
        if (review.getReplyStatus() == null) {
            review.setReplyStatus(0);
        }
        reviewMapper.insert(review);
        log.info("新增评价: userId={}, productId={}, parentId={}",
                review.getUserId(), review.getProductId(), review.getParentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, String reply) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        review.setReply(reply);
        review.setReplyTime(LocalDateTime.now());
        review.setReplyStatus(1);
        review.setUpdateTime(LocalDateTime.now());
        reviewMapper.updateById(review);
        log.info("回复评价: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        reviewMapper.deleteById(id);
        log.info("删除评价: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        review.setStatus(status);
        review.setUpdateTime(LocalDateTime.now());
        reviewMapper.updateById(review);
        log.info("更新评价状态: id={}, status={}", id, status);
    }
}
