package com.mall.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    private String nickname;
    private Integer gender;
    private Integer status;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
