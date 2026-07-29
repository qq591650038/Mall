package com.mall.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data public class LoginDTO { @NotBlank private String account; @NotBlank private String password; private Integer loginType; private String verifyCode; private String verifyKey; }
