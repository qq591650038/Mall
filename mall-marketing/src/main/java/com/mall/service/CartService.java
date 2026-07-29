package com.mall.service;

import com.mall.entity.Cart;
import com.mall.vo.CartVO;
import java.util.List;

public interface CartService {
    List<CartVO> listByUserId(Long userId);
    void add(Long userId, Long skuId, Integer quantity);
    void updateQuantity(Long cartId, Long userId, Integer quantity);
    void delete(Long cartId, Long userId);
    void batchDelete(List<Long> cartIds, Long userId);
    void selectAll(Long userId, Boolean selected);
}
