package com.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.PointsLedger;
import com.mall.entity.PointsProduct;
import com.mall.entity.PointsRedemption;
import com.mall.vo.PointsSummaryVO;
import com.mall.dto.points.PointsRedeemDTO;

public interface PointsService {
    PointsSummaryVO getSummary(Long userId);

    Page<PointsLedger> pageLedger(Long userId, Integer current, Integer size);

    PointsSummaryVO checkIn(Long userId);

    java.util.List<PointsProduct> listProducts();

    PointsRedemption redeem(Long userId, Long productId, PointsRedeemDTO dto);

    Page<PointsRedemption> pageRedemptions(Long userId, Integer current, Integer size);

    /** 赚取积分（支付成功后调用） */
    void earn(Long userId, int points, String remark, Long businessId);

    /** 按用户当前会员等级倍率发放订单支付积分。 */
    void earnForPayment(Long userId, java.math.BigDecimal paidAmount, Long orderId, String orderNo);

    /** 全额退款后撤销该订单已发放的支付积分。 */
    void reversePaymentPoints(Long userId, Long orderId, String orderNo);

    /** 根据积分自动升级会员等级 */
    void autoUpgradeLevel(Long userId);
}
