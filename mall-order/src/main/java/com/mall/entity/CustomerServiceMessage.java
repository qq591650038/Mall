package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_service_message")
public class CustomerServiceMessage {
    @TableId(type = IdType.AUTO) private Long id;
    private Long ticketId;
    private Long senderId;
    private String senderRole;
    private String content;
    private LocalDateTime createTime;
}
