package com.mall.vo;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
@Data
public class CartVO implements Serializable { private Long id; private Long productId; private Long skuId; private String productName; private String productImage; private String skuInfo; private BigDecimal price; private Integer stock; private Integer quantity; private Integer selected; private Boolean outOfStock; }
