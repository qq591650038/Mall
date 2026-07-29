package com.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("product")
public class Product implements Serializable {

    @TableField(exist = false)
    private List<ProductSku> skus;

    @TableField(exist = false)
    private Long reviewCount;

    @TableField(exist = false)
    private Double averageRating;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;
    private Long brandId;
    private String name;
    private String subtitle;
    private String mainImage;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer totalStock;
    private Integer sales;
    private Integer status;
    private Integer isRecommend;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
