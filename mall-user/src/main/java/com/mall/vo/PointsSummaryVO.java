package com.mall.vo;

import lombok.Data;

@Data
public class PointsSummaryVO {
    private Integer balance;
    private Integer totalEarned;
    private Integer totalSpent;
    private String memberLevel;
    /** 当前会员等级ID */
    private Long memberLevelId;
    /** 当前等级积分倍率 */
    private java.math.BigDecimal pointsRate;
    /** 下一等级积分要求（0表示已满级） */
    private Integer nextLevelPoints;
    /** 下一等级名称 */
    private String nextLevelName;
    /** 距离下一等级还差多少积分 */
    private Integer pointsToNextLevel;
    private Boolean checkedInToday;
}
