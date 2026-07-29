package com.mall.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.result.ErrorCode;
import com.mall.common.result.OrderStatus;
import com.mall.entity.Address;
import com.mall.entity.Coupon;
import com.mall.entity.Order;
import com.mall.entity.OrderItem;
import com.mall.entity.PointsProduct;
import com.mall.entity.Product;
import com.mall.entity.ProductSku;
import com.mall.entity.UserCoupon;
import com.mall.exception.BusinessException;
import com.mall.mapper.AddressMapper;
import com.mall.mapper.CouponMapper;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.service.CouponService;
import com.mall.service.InventoryService;
import com.mall.service.PointsRewardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PointsRewardServiceImpl implements PointsRewardService {
    private static final Set<String> ORDER_REWARD_TYPES = Set.of("PHYSICAL", "VIRTUAL", "SERVICE");

    private final CouponMapper couponMapper;
    private final CouponService couponService;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final AddressMapper addressMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public PointsRewardServiceImpl(CouponMapper couponMapper, CouponService couponService,
                                   ProductMapper productMapper, ProductSkuMapper skuMapper,
                                   AddressMapper addressMapper, OrderMapper orderMapper,
                                   OrderItemMapper orderItemMapper, InventoryService inventoryService,
                                   ObjectMapper objectMapper) {
        this.couponMapper = couponMapper;
        this.couponService = couponService;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.addressMapper = addressMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void validateConfiguration(PointsProduct pointsProduct) {
        String type = pointsProduct.getRewardType();
        if ("COUPON".equals(type)) {
            Coupon coupon = pointsProduct.getRewardRefId() == null ? null : couponMapper.selectById(pointsProduct.getRewardRefId());
            if (coupon == null || !Objects.equals(coupon.getStatus(), 1)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "积分奖励关联的优惠券不存在或已停用");
            }
            return;
        }
        if (!ORDER_REWARD_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的积分奖励类型");
        }
        Product product = pointsProduct.getRewardRefId() == null ? null : productMapper.selectById(pointsProduct.getRewardRefId());
        ProductSku sku = pointsProduct.getRewardSkuId() == null ? null : skuMapper.selectById(pointsProduct.getRewardSkuId());
        if (product == null || sku == null || !Objects.equals(sku.getProductId(), product.getId())
                || !Objects.equals(product.getStatus(), 1) || !Objects.equals(sku.getStatus(), 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "积分奖励关联的商品或SKU不存在、已下架或不匹配");
        }
    }

    @Override
    public FulfillmentResult fulfill(Long userId, PointsProduct pointsProduct, Long addressId) {
        validateConfiguration(pointsProduct);
        if ("COUPON".equals(pointsProduct.getRewardType())) {
            UserCoupon userCoupon = couponService.grantForPoints(userId, pointsProduct.getRewardRefId());
            return new FulfillmentResult(null, userCoupon.getId(), "ISSUED");
        }
        Address address = null;
        if ("PHYSICAL".equals(pointsProduct.getRewardType())) {
            address = addressId == null ? null : addressMapper.selectById(addressId);
            if (address == null || !Objects.equals(address.getUserId(), userId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "兑换实物奖励时请选择有效收货地址");
            }
        }
        Product product = productMapper.selectById(pointsProduct.getRewardRefId());
        ProductSku sku = skuMapper.selectById(pointsProduct.getRewardSkuId());
        inventoryService.reserve(sku.getId(), product.getId(), 1);

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setOrderNo("PR" + now.toString().replaceAll("[-T:.]", "") + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setUserId(userId);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setPayAmount(BigDecimal.ZERO);
        order.setPayStatus(1);
        order.setPayTime(now);
        order.setOrderStatus("VIRTUAL".equals(pointsProduct.getRewardType())
                ? OrderStatus.COMPLETED.getCode() : OrderStatus.PAID.getCode());
        order.setAddressId(address == null ? null : address.getId());
        order.setAddressSnapshot(address == null ? null : addressSnapshot(address));
        order.setOrderSource("POINTS");
        order.setOrderType(pointsProduct.getRewardType());
        order.setRemark("积分兑换：" + pointsProduct.getName());
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setDeleted(0);
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setSkuId(sku.getId());
        item.setProductName(product.getName());
        item.setSkuInfo(sku.getSpecInfo());
        item.setProductImage(sku.getImage() == null ? product.getMainImage() : sku.getImage());
        item.setPrice(BigDecimal.ZERO);
        item.setQuantity(1);
        item.setSubtotal(BigDecimal.ZERO);
        item.setCreateTime(now);
        orderItemMapper.insert(item);
        return new FulfillmentResult(order.getId(), null, "ORDER_CREATED");
    }

    private String addressSnapshot(Address address) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "receiverName", address.getReceiverName(), "receiverPhone", address.getReceiverPhone(),
                    "province", address.getProvince(), "city", address.getCity(),
                    "district", address.getDistrict(), "detailAddress", address.getDetailAddress()));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "收货地址数据无效");
        }
    }
}
