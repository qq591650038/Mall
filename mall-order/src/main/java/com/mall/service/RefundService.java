package com.mall.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.dto.order.RefundCallbackDTO;
import com.mall.entity.Refund;
import com.mall.vo.RefundVO;
public interface RefundService {
    RefundVO apply(Long userId, Long orderId, Refund refund);
    void applyGroupFailureRefund(Long orderId);
    RefundVO getById(Long id, Long userId);
    Page<RefundVO> pageByUserId(Long userId, Integer current, Integer size, Integer status);
    Page<RefundVO> pageAdmin(Integer current, Integer size, Integer status, String orderNo);

    com.mall.common.result.CursorPageResult<RefundVO> cursorPageAdmin(Integer size, Integer status, String orderNo, String cursor);
    void review(Long id, Integer status, String remark);
    void refundSuccess(Long id); void cancel(Long id, Long userId); void refundFailed(Long id, String reason); void callback(RefundCallbackDTO callback);

    void submitReturnLogistics(Long userId, Long refundId, String logisticsCompany, String logisticsNo);
    void updateReturnLogistics(Long refundId, String logisticsCompany, String logisticsNo);
    void updateExchangeLogistics(Long refundId, String trackingNo);
    void confirmReturnReceived(Long userId, Long refundId);
    RefundVO applyExchange(Long userId, Long orderId, Long exchangeProductId, Long exchangeSkuId, String reason);
}
