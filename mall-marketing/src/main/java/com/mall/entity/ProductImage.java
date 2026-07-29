package com.mall.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.io.Serializable; import java.time.LocalDateTime;
@Data @TableName("product_image") public class ProductImage implements Serializable { @TableId(type=IdType.AUTO) private Long id; private Long productId; private String url; private Integer sort; @TableField(fill=FieldFill.INSERT) private LocalDateTime createTime; }
