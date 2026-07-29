package com.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.MarketingActivity;
import com.mall.entity.MarketingActivityItem;
import com.mall.entity.MarketingParticipant;
import com.mall.vo.MarketingActivityItemVO;
import com.mall.vo.MarketingParticipateVO;
import com.mall.vo.MarketingGroupVO;
import com.mall.vo.GroupPaymentResult;

import java.util.List;

/**
 * 营销活动服务接口
 */
public interface MarketingActivityService {

    /** 获取活动列表（分页） */
    Page<MarketingActivity> page(Integer current, Integer size, String type, Integer status);

    /** 获取进行中的活动列表 */
    List<MarketingActivity> listActive(String type);

    /** 根据ID获取活动详情 */
    MarketingActivity getById(Long id);

    /** 创建活动 */
    MarketingActivity create(MarketingActivity activity, List<MarketingActivityItem> items);

    /** 更新活动 */
    MarketingActivity update(Long id, MarketingActivity activity, List<MarketingActivityItem> items);

    /** 删除活动 */
    void delete(Long id);

    /** 取消活动 */
    void cancel(Long id);

    /** 获取活动商品明细（含商品信息） */
    List<MarketingActivityItemVO> listItems(Long activityId);

    /** 获取活动商品数量 */
    long countItems(Long activityId);

    /** 参与活动（下单） */
    MarketingParticipateVO participate(Long activityId, Long itemId, Long userId, Integer quantity);

    /** 支付成功回调 */
    void onPaymentSuccess(Long participantId, Long orderId);

    /** 订单取消回调（释放库存） */
    void onOrderCancel(Long participantId);
    void linkOrder(Long participantId, Long orderId);
    GroupPaymentResult onPaymentSuccessByOrderId(Long orderId);
    void onOrderCancelByOrderId(Long orderId);
    void onRefundSuccessByOrderId(Long orderId);
    List<Long> expireUnformedGroups();
    List<MarketingGroupVO> listGroups(Long activityId);

    /** 更新活动状态（定时任务调用） */
    void updateActivityStatus();
}
