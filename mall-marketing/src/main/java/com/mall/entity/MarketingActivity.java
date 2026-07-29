package com.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 营销活动主表实体类
 */
@Data
@TableName("marketing_activity")
public class MarketingActivity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动名称 */
    private String name;

    /** 活动类型: LIMIT_TIME_DISCOUNT-限时折扣, FULL_REDUCTION-满减, SECKILL-秒杀, GROUP_BUY-拼团 */
    private String type;
    private Integer groupTarget;

    /** 活动描述 */
    private String description;

    /** 活动开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 活动结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 状态: 0-未开始, 1-进行中, 2-已结束, 3-已取消 */
    private Integer status;

    /** 排序 */
    private Integer sort;

    /** 创建人ID */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    /** 商品数量（非数据库字段，由查询填充） */
    @TableField(exist = false)
    private Long itemCount;
}
