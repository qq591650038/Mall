package com.mall.entity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order implements Serializable {
    @TableId(type = IdType.AUTO) private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal payAmount;
    private Integer payStatus;
    private LocalDateTime payTime;
    private Integer orderStatus;
    private LocalDateTime shipTime;
    private LocalDateTime receiveTime;
    private Long addressId;
    private String addressSnapshot;
    private String logisticsCompany;
    private String logisticsNo;
    private LocalDateTime autoConfirmDeadline;
    private LocalDateTime expireTime;
    private String remark;
    private Long couponId;
    private String orderSource;
    private String orderType;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic @TableField(fill = FieldFill.INSERT) private Integer deleted;
}
