package com.mall.dto;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class RegisterDTO { @NotBlank @Size(min=3,max=20) private String username; @Pattern(regexp="^1[3-9]\\d{9}$") private String phone; @Email private String email; @NotBlank @Size(min=6,max=20) private String password; @NotBlank private String confirmPassword; @NotBlank private String verifyCode; @NotBlank private String verifyKey; }
