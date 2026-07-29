package com.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 会员等级实体 */
@Data
@TableName("member_level")
public class MemberLevel implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 等级名称 */
    private String name;

    /** 等级数值（1-初级, 2-中级, 3-高级, 4-VIP） */
    private Integer level;

    /** 达到该等级所需最少积分 */
    private Integer minPoints;

    /** 达到该等级所需最多积分（0表示无上限） */
    private Integer maxPoints;

    /** 积分倍率（1.0=1倍, 1.5=1.5倍） */
    private BigDecimal pointsRate;

    /** 折扣率（0.9=9折, 1.0=无折扣） */
    private BigDecimal discountRate;

    /** 等级图标 */
    private String icon;

    /** 等级描述/权益说明 */
    private String description;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 排序 */
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
