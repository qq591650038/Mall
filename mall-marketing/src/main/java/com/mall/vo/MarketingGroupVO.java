package com.mall.vo;

import lombok.Data;
import java.util.List;

@Data
public class MarketingGroupVO {
    private String groupNo;
    private Integer target;
    private Integer joinedQuantity;
    private Integer groupStatus;
    private List<MarketingGroupMemberVO> members;
}
