package com.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("browse_history")
public class BrowseHistory implements Serializable {

    @TableField(exist = false)
    private Product product;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long productId;
    private LocalDateTime browseTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
