package com.mall.vo;

import lombok.Data;

@Data
public class MarketingGroupMemberVO {
    private Long participantId;
    private Long userId;
    private String username;
    private Integer quantity;
    private Long orderId;
    private String orderNo;
    private Integer participantStatus;
    private Integer orderStatus;
    private String orderStatusText;
    private Integer payStatus;
}
