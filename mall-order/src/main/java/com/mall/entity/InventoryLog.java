package com.mall.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("inventory_log") public class InventoryLog { @TableId(type=IdType.AUTO) private Long id; private Long skuId; private Long productId; private Long orderId; private Integer quantity; private String operation; private Integer status; private Integer retryCount; private String errorMessage; private LocalDateTime createTime; private LocalDateTime updateTime; }
