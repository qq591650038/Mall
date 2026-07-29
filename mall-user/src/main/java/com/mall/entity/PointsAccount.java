package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 用户积分账户，余额通过流水变更并在账户表中做汇总。 */
@Data
@TableName("points_account")
public class PointsAccount implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 会员等级ID */
    private Long memberLevelId;
    private Integer balance;
    private Integer totalEarned;
    private Integer totalSpent;
    private LocalDateTime updateTime;
}
