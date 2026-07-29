package com.mall.vo;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class RefundVO implements Serializable {
    private Long id;
    private Long orderId;
    private String orderNo;
    private String refundNo;
    private BigDecimal amount;
    private String reason;
    private String images;
    private Integer status;
    private String statusText;
    private String username;
    private String reviewRemark;
    private LocalDateTime reviewTime;
    private String paymentNo;
    private LocalDateTime createTime;
    private OrderVO orderInfo;
    private Integer type;
    private String logisticsCompany;
    private String logisticsNo;
    private String returnAddress;
    private String trackingNo;
    private Long exchangeProductId;
    private Long exchangeSkuId;
}