package com.mall.dto.order;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data public class RefundCallbackDTO { @NotBlank private String refundNo; @NotBlank private String orderNo; @NotBlank private String amount; @NotBlank private String timestamp; @NotBlank private String status; @NotBlank private String signature; }
