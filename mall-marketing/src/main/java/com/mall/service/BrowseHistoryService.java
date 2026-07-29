package com.mall.service;

import com.mall.entity.BrowseHistory;
import java.util.List;

public interface BrowseHistoryService {
    List<BrowseHistory> listByUserId(Long userId);
    void add(Long userId, Long productId);
    void delete(Long userId, Long productId);
    void clear(Long userId);
}
