package com.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 营销活动参与者实体类
 */
@Data
@TableName("marketing_participant")
public class MarketingParticipant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;

    /** 活动ID */
    private Long activityId;

    /** 活动商品明细ID */
    private Long activityItemId;

    /** 用户ID */
    private Long userId;

    /** 订单ID */
    private Long orderId;

    /** 购买数量 */
    private Integer quantity;

    /** 拼团编号（拼团活动使用） */
    private String groupNo;

    /** 拼团状态: 1-拼团中, 2-拼团成功, 3-拼团失败 */
    private Integer groupStatus;

    /** 父参与者ID（拼团团长为null，团员为团长ID） */
    private Long parentId;

    /** 状态: 0-待支付, 1-已支付, 2-已取消 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
