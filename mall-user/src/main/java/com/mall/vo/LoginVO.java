package com.mall.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class LoginVO implements Serializable {
    private String token;
    private Long expiresIn;
    private UserInfoVO userInfo;

    @Data
    public static class UserInfoVO implements Serializable {
        private Long id;
        private String username;
        private String phone;
        private String email;
        private String avatar;
        private String nickname;
    }
}
