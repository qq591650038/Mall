package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mall.common.result.ErrorCode;
import com.mall.entity.Cart;
import com.mall.entity.Product;
import com.mall.entity.ProductSku;
import com.mall.exception.BusinessException;
import com.mall.mapper.CartMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.service.CartService;
import com.mall.vo.CartVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;

    public CartServiceImpl(CartMapper cartMapper,
                           ProductSkuMapper productSkuMapper,
                           ProductMapper productMapper) {
        this.cartMapper = cartMapper;
        this.productSkuMapper = productSkuMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<CartVO> listByUserId(Long userId) {
        List<Cart> carts = cartMapper.selectList(
                new QueryWrapper<Cart>().eq("user_id", userId).orderByDesc("create_time")
        );
        if (carts.isEmpty()) {
            return List.of();
        }

        List<Long> skuIds = carts.stream().map(Cart::getSkuId).distinct().collect(Collectors.toList());
        List<Long> productIds = carts.stream().map(Cart::getProductId).distinct().collect(Collectors.toList());

        Map<Long, ProductSku> skuMap = productSkuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return carts.stream()
                .map(cart -> convertToVO(cart, skuMap, productMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, Long skuId, Integer quantity) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ErrorCode.SKU_NOT_EXIST);
        }
        if (sku.getStock() < quantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }

        Cart exist = cartMapper.selectOne(
                new QueryWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId)
        );

        if (exist != null) {
            int newQuantity = exist.getQuantity() + quantity;
            if (sku.getStock() < newQuantity) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
            }
            exist.setQuantity(newQuantity);
            exist.setUpdateTime(LocalDateTime.now());
            cartMapper.updateById(exist);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(sku.getProductId());
            cart.setSkuId(skuId);
            cart.setQuantity(quantity);
            cart.setSelected(1);
            cart.setCreateTime(LocalDateTime.now());
            cart.setUpdateTime(LocalDateTime.now());
            cart.setDeleted(0);
            cartMapper.insert(cart);
        }
        log.info("加入购物车: userId={}, skuId={}, quantity={}", userId, skuId, quantity);
    }

    @Override
    public void updateQuantity(Long cartId, Long userId, Integer quantity) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CART_NOT_EXIST);
        }
        ProductSku sku = productSkuMapper.selectById(cart.getSkuId());
        if (sku.getStock() < quantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }
        cart.setQuantity(quantity);
        cart.setUpdateTime(LocalDateTime.now());
        cartMapper.updateById(cart);
    }

    @Override
    public void delete(Long cartId, Long userId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CART_NOT_EXIST);
        }
        cartMapper.deleteById(cartId);
        log.info("删除购物车: cartId={}", cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> cartIds, Long userId) {
        List<Cart> carts = cartMapper.selectBatchIds(cartIds);
        for (Cart cart : carts) {
            if (!cart.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.CART_NOT_EXIST);
            }
        }
        cartMapper.deleteBatchIds(cartIds);
    }

    @Override
    public void selectAll(Long userId, Boolean selected) {
        int selectedValue = selected ? 1 : 0;
        UpdateWrapper<Cart> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId)
                .set("selected", selectedValue)
                .set("update_time", LocalDateTime.now());
        cartMapper.update(null, wrapper);
    }

    private CartVO convertToVO(Cart cart, Map<Long, ProductSku> skuMap, Map<Long, Product> productMap) {
        CartVO vo = new CartVO();
        vo.setId(cart.getId());
        vo.setProductId(cart.getProductId());
        vo.setSkuId(cart.getSkuId());
        vo.setQuantity(cart.getQuantity());
        vo.setSelected(cart.getSelected());

        ProductSku sku = skuMap.get(cart.getSkuId());
        if (sku != null) {
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
            vo.setSkuInfo(sku.getSpecInfo());
            vo.setProductImage(sku.getImage());
            vo.setOutOfStock(sku.getStock() <= 0);
        }

        Product product = productMap.get(cart.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            if (vo.getProductImage() == null) {
                vo.setProductImage(product.getMainImage());
            }
        }

        return vo;
    }
}
