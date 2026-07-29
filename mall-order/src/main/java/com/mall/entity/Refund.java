package com.mall.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("refund")
public class Refund implements Serializable {
    @TableId(type = IdType.AUTO) private Long id; private Long orderId; private String orderNo; private Long userId; private String refundNo; private BigDecimal amount; private String reason; private String images; private Integer status; private String reviewRemark; private LocalDateTime reviewTime; private String paymentNo; private Integer retryCount; private String lastError;
    private Integer type;
    private Integer originalOrderStatus;
    private String logisticsCompany;
    private String logisticsNo;
    private String returnAddress;
    private String trackingNo;
    private Long exchangeProductId;
    private Long exchangeSkuId;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic @TableField(fill = FieldFill.INSERT) private Integer deleted;
}
