package com.mall.vo.admin;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class AdminLoginVO implements Serializable {

    private String token;
    private Long expiresIn;
    private AdminInfoVO adminInfo;

    @Data
    public static class AdminInfoVO implements Serializable {
        private Long id;
        private String username;
        private String realName;
        private String avatar;
        private String email;
        private String phone;
        private List<String> roles;
        private List<String> permissions;
    }
}