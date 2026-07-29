package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.dto.order.CreateOrderDTO;
import com.mall.entity.Address;
import com.mall.service.AddressService;
import com.mall.service.MarketingActivityService;
import com.mall.service.OrderService;
import com.mall.vo.MarketingParticipateVO;
import com.mall.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 营销活动订单控制器
 * 处理用户参与营销活动并创建订单的完整流程
 */
@RestController
@RequestMapping("/api/marketing")
@Tag(name = "营销活动订单", description = "营销活动下单接口")
public class MarketingOrderController {

    private final MarketingActivityService marketingActivityService;
    private final OrderService orderService;
    private final AddressService addressService;

    public MarketingOrderController(MarketingActivityService marketingActivityService,
                                     OrderService orderService,
                                     AddressService addressService) {
        this.marketingActivityService = marketingActivityService;
        this.orderService = orderService;
        this.addressService = addressService;
    }

    @PostMapping("/participate")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "参与活动并下单", description = "用户参与营销活动并自动创建订单")
    public Result<MarketingParticipateVO> participate(@AuthenticationPrincipal Long userId,
                                                      @RequestBody ParticipateRequest request) {
        // 1. 参与活动（扣减库存、创建参与记录）
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        MarketingParticipateVO participateResult = marketingActivityService.participate(
                request.getActivityId(),
                request.getItemId(),
                userId,
                quantity
        );

        // 2. 获取用户默认地址
        List<Address> addresses = addressService.listByUserId(userId);
        Address defaultAddress = addresses.stream()
                .filter(a -> a.getIsDefault() != null && a.getIsDefault() == 1)
                .findFirst()
                .orElse(addresses.isEmpty() ? null : addresses.get(0));

        // 如果没有默认地址，使用参与时的 itemId 对应的商品创建订单
        // 注：此处简化处理，使用活动商品的 productId 和 skuId
        Long productId = participateResult.getProductId();
        Long skuId = participateResult.getSkuId();
        if (productId == null || skuId == null) {
            throw new IllegalArgumentException("活动商品必须绑定有效 SKU");
        }

        // 3. 创建订单
        CreateOrderDTO orderDTO = new CreateOrderDTO();
        if (defaultAddress != null) {
            orderDTO.setAddressId(defaultAddress.getId());
        }
        orderDTO.setRemark("参与营销活动 #" + request.getActivityId());

        CreateOrderDTO.OrderItemDTO orderItemDTO = new CreateOrderDTO.OrderItemDTO();
        orderItemDTO.setProductId(productId);
        orderItemDTO.setSkuId(skuId);
        orderItemDTO.setQuantity(quantity);
        orderDTO.setItems(List.of(orderItemDTO));

        OrderVO orderVO = orderService.createMarketingOrder(
                userId, orderDTO, Map.of(skuId, participateResult.getActivityPrice()));

        marketingActivityService.linkOrder(participateResult.getParticipantId(), orderVO.getId());

        // 4. 关联订单到参与记录
        // The participant remains pending until the payment callback succeeds.

        // 5. 返回结果（含订单信息）
        participateResult.setOrderId(orderVO.getId());
        participateResult.setOrderNo(orderVO.getOrderNo());
        return Result.success(participateResult);
    }

    /**
     * 参与活动请求体
     */
    public static class ParticipateRequest {
        private Long activityId;
        private Long itemId;
        private Long productId;
        private Long skuId;
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

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
