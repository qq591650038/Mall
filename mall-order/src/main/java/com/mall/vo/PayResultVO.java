package com.mall.vo;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
@Data
public class PayResultVO implements Serializable { private Long orderId; private String orderNo; private String paymentNo; private BigDecimal amount; private Integer paymentMethod; private Integer paymentStatus; private Integer expireSeconds; private String payUrl; }
