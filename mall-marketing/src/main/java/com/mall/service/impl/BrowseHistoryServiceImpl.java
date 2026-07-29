package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.entity.BrowseHistory;
import com.mall.mapper.BrowseHistoryMapper;
import com.mall.mapper.ProductMapper;
import com.mall.service.BrowseHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    private final BrowseHistoryMapper browseHistoryMapper;
    private final ProductMapper productMapper;

    public BrowseHistoryServiceImpl(BrowseHistoryMapper browseHistoryMapper, ProductMapper productMapper) {
        this.browseHistoryMapper = browseHistoryMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<BrowseHistory> listByUserId(Long userId) {
        List<BrowseHistory> list = browseHistoryMapper.selectList(
                new QueryWrapper<BrowseHistory>().eq("user_id", userId).orderByDesc("browse_time")
        );
        var productIds = list.stream().map(BrowseHistory::getProductId).collect(Collectors.toSet());
        var products = productIds.isEmpty() ? Collections.<Long, com.mall.entity.Product>emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(com.mall.entity.Product::getId, Function.identity()));
        list.forEach(item -> item.setProduct(products.get(item.getProductId())));
        return list;
    }

    @Override
    public void add(Long userId, Long productId) {
        BrowseHistory exist = browseHistoryMapper.selectOne(
                new QueryWrapper<BrowseHistory>().eq("user_id", userId).eq("product_id", productId)
        );
        if (exist != null) {
            exist.setBrowseTime(LocalDateTime.now());
            browseHistoryMapper.updateById(exist);
        } else {
            BrowseHistory history = new BrowseHistory();
            history.setUserId(userId);
            history.setProductId(productId);
            history.setBrowseTime(LocalDateTime.now());
            history.setCreateTime(LocalDateTime.now());
            browseHistoryMapper.insert(history);
        }
    }

    @Override
    public void delete(Long userId, Long productId) {
        browseHistoryMapper.delete(
                new QueryWrapper<BrowseHistory>().eq("user_id", userId).eq("product_id", productId)
        );
    }

    @Override
    public void clear(Long userId) {
        browseHistoryMapper.delete(
                new QueryWrapper<BrowseHistory>().eq("user_id", userId)
        );
    }
}
