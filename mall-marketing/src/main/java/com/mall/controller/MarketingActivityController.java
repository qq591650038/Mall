package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.MarketingActivity;
import com.mall.entity.MarketingActivityItem;
import com.mall.service.MarketingActivityService;
import com.mall.vo.MarketingActivityItemVO;
import com.mall.vo.MarketingParticipateVO;
import com.mall.vo.MarketingGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销活动控制器
 */
@RestController
@RequestMapping("/api/marketing")
@Tag(name = "营销活动", description = "营销活动接口")
public class MarketingActivityController {

    private final MarketingActivityService marketingActivityService;

    public MarketingActivityController(MarketingActivityService marketingActivityService) {
        this.marketingActivityService = marketingActivityService;
    }

    // ==================== 用户端接口 ====================

    @GetMapping("/activities/active")
    @Operation(summary = "获取进行中的活动", description = "获取当前进行中的营销活动列表")
    public Result<List<MarketingActivity>> listActive(@RequestParam(required = false) String type) {
        List<MarketingActivity> list = marketingActivityService.listActive(type);
        return Result.success(list);
    }

    @GetMapping("/activities/{id}")
    @Operation(summary = "获取活动详情", description = "获取活动详情")
    public Result<MarketingActivity> getById(@PathVariable Long id) {
        MarketingActivity activity = marketingActivityService.getById(id);
        return Result.success(activity);
    }

    @GetMapping("/activities/{id}/items")
    @Operation(summary = "获取活动商品", description = "获取活动的商品明细列表（含商品信息）")
    public Result<List<MarketingActivityItemVO>> listItems(@PathVariable Long id) {
        List<MarketingActivityItemVO> list = marketingActivityService.listItems(id);
        return Result.success(list);
    }

    // ==================== 管理端接口 ====================

    @GetMapping("/admin/activities")
    @Operation(summary = "活动列表", description = "管理端获取活动列表（分页，含商品数量）")
    public Result<PageResult<MarketingActivity>> page(@RequestParam(defaultValue = "1") Integer current,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) String type,
                                                        @RequestParam(required = false) Integer status) {
        Page<MarketingActivity> page = marketingActivityService.page(current, size, type, status);
        PageResult<MarketingActivity> result = new PageResult<>(page.getTotal(), page.getRecords(), current, size);
        return Result.success(result);
    }

    @PostMapping("/admin/activities")
    @Operation(summary = "创建活动", description = "创建营销活动")
    public Result<MarketingActivity> create(@AuthenticationPrincipal Long adminId,
                                            @RequestBody CreateActivityRequest request) {
        MarketingActivity activity = new MarketingActivity();
        activity.setName(request.getName());
        activity.setType(request.getType());
        activity.setGroupTarget(request.getGroupTarget());
        activity.setDescription(request.getDescription());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setSort(request.getSort());
        activity.setCreatedBy(adminId);

        List<MarketingActivityItem> items = request.getItems().stream().map(itemRequest -> {
            MarketingActivityItem item = new MarketingActivityItem();
            item.setProductId(itemRequest.getProductId());
            item.setSkuId(itemRequest.getSkuId());
            item.setActivityPrice(itemRequest.getActivityPrice());
            item.setOriginalPrice(itemRequest.getOriginalPrice());
            item.setStock(itemRequest.getStock());
            item.setLimitPerUser(itemRequest.getLimitPerUser());
            return item;
        }).toList();

        MarketingActivity result = marketingActivityService.create(activity, items);
        return Result.success(result);
    }

    @PutMapping("/admin/activities/{id}")
    @Operation(summary = "更新活动", description = "更新营销活动")
    public Result<MarketingActivity> update(@PathVariable Long id,
                                            @RequestBody CreateActivityRequest request) {
        MarketingActivity activity = new MarketingActivity();
        activity.setName(request.getName());
        activity.setType(request.getType());
        activity.setGroupTarget(request.getGroupTarget());
        activity.setDescription(request.getDescription());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setSort(request.getSort());

        List<MarketingActivityItem> items = request.getItems().stream().map(itemRequest -> {
            MarketingActivityItem item = new MarketingActivityItem();
            item.setProductId(itemRequest.getProductId());
            item.setSkuId(itemRequest.getSkuId());
            item.setActivityPrice(itemRequest.getActivityPrice());
            item.setOriginalPrice(itemRequest.getOriginalPrice());
            item.setStock(itemRequest.getStock());
            item.setLimitPerUser(itemRequest.getLimitPerUser());
            return item;
        }).toList();

        MarketingActivity result = marketingActivityService.update(id, activity, items);
        return Result.success(result);
    }

    @DeleteMapping("/admin/activities/{id}")
    @Operation(summary = "删除活动", description = "删除营销活动")
    public Result<Void> delete(@PathVariable Long id) {
        marketingActivityService.delete(id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/admin/activities/{id}/cancel")
    @Operation(summary = "取消活动", description = "取消进行中的营销活动")
    public Result<Void> cancel(@PathVariable Long id) {
        marketingActivityService.cancel(id);
        return Result.success("取消成功", null);
    }

    @GetMapping("/admin/activities/{id}/groups")
    @Operation(summary = "查看拼团明细")
    public Result<List<MarketingGroupVO>> groups(@PathVariable Long id) {
        return Result.success(marketingActivityService.listGroups(id));
    }

    /**
     * 参与活动请求体
     */
    public static class ParticipateRequest {
        private Long activityId;
        private Long itemId;
        private Integer quantity = 1;

        public Long getActivityId() {
            return activityId;
        }

        public void setActivityId(Long activityId) {
            this.activityId = activityId;
        }

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * 创建活动请求体
     */
    public static class CreateActivityRequest {
        private String name;
        private String type;
        private Integer groupTarget;
        private String description;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        private Integer sort;
        private List<ActivityItemRequest> items;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getGroupTarget() { return groupTarget; }
        public void setGroupTarget(Integer groupTarget) { this.groupTarget = groupTarget; }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(java.time.LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(java.time.LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }

        public List<ActivityItemRequest> getItems() {
            return items;
        }

        public void setItems(List<ActivityItemRequest> items) {
            this.items = items;
        }
    }

    /**
     * 活动商品请求体
     */
    public static class ActivityItemRequest {
        private Long productId;
        private Long skuId;
        private BigDecimal activityPrice;
        private BigDecimal originalPrice;
        private Integer stock;
        private Integer limitPerUser = 1;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Long getSkuId() {
            return skuId;
        }

        public void setSkuId(Long skuId) {
            this.skuId = skuId;
        }

        public BigDecimal getActivityPrice() {
            return activityPrice;
        }

        public void setActivityPrice(BigDecimal activityPrice) {
            this.activityPrice = activityPrice;
        }

        public BigDecimal getOriginalPrice() {
            return originalPrice;
        }

        public void setOriginalPrice(BigDecimal originalPrice) {
            this.originalPrice = originalPrice;
        }

        public Integer getStock() {
            return stock;
        }

        public void setStock(Integer stock) {
            this.stock = stock;
        }

        public Integer getLimitPerUser() {
            return limitPerUser;
        }

        public void setLimitPerUser(Integer limitPerUser) {
            this.limitPerUser = limitPerUser;
        }
    }
}
