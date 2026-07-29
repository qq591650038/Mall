package com.mall.scheduler;

import com.mall.service.MarketingActivityService;
import com.mall.service.RefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 营销活动状态更新定时任务
 * 定期检查活动时间，自动更新活动状态
 */
@Slf4j
@Component
public class MarketingActivityScheduler {

    private final MarketingActivityService marketingActivityService;
    private final RefundService refundService;

    public MarketingActivityScheduler(MarketingActivityService marketingActivityService,
                                      RefundService refundService) {
        this.marketingActivityService = marketingActivityService;
        this.refundService = refundService;
    }

    /**
     * 更新活动状态 - 每5分钟执行一次
     * 将未开始的活动更新为进行中，将进行中的活动更新为已结束
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void updateActivityStatus() {
        log.info("开始执行营销活动状态更新...");
        try {
            marketingActivityService.updateActivityStatus();
            for (Long orderId : marketingActivityService.expireUnformedGroups()) {
                try {
                    refundService.applyGroupFailureRefund(orderId);
                } catch (Exception e) {
                    log.error("Failed to create group-buy refund, orderId={}", orderId, e);
                }
            }
            log.info("营销活动状态更新完成");
        } catch (Exception e) {
            log.error("营销活动状态更新异常", e);
        }
    }
}
