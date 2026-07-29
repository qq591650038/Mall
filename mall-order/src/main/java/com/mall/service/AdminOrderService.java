package com.mall.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.OrderItem;
import com.mall.vo.OrderVO;
import java.util.List;

public interface AdminOrderService {
    Page<OrderVO> page(Integer current, Integer size, Integer status, String orderNo, Long userId);

    com.mall.common.result.CursorPageResult<OrderVO> cursorPage(Integer size, Integer status, String orderNo, Long userId, String cursor);

    OrderVO getDetail(Long id);

    List<OrderItem> items(Long id);

    void ship(Long id, String logisticsCompany, String logisticsNo);

    void updateStatus(Long id, Integer status);
}
