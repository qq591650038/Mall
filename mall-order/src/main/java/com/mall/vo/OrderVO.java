package com.mall.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO implements Serializable {
    private Long id; private String orderNo; private String username;
    private BigDecimal totalAmount; private BigDecimal discountAmount; private BigDecimal freightAmount; private BigDecimal payAmount;
    private Integer payStatus; private LocalDateTime payTime; private Integer orderStatus; private String orderStatusText;
    private LocalDateTime shipTime; private LocalDateTime receiveTime; private String logisticsCompany; private String logisticsNo;
    private LocalDateTime autoConfirmDeadline; private LocalDateTime expireTime; private Integer timeoutHours; private String remark;
    private LocalDateTime createTime; private LocalDateTime updateTime; private AddressSnapshot addressSnapshot; private List<OrderItemVO> items; private List<OrderTimelineVO> timeline;
    @Data public static class AddressSnapshot implements Serializable { private String receiverName; private String receiverPhone; private String province; private String city; private String district; private String detailAddress; }
    @Data public static class OrderItemVO implements Serializable { private Long id; private Long productId; private Long skuId; private String productName; private String skuInfo; private String productImage; private BigDecimal price; private Integer quantity; private BigDecimal subtotal; }
    @Data public static class OrderTimelineVO implements Serializable { private Integer status; private String statusText; private LocalDateTime time; private String description; }
}
