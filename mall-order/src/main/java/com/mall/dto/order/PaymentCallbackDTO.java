package com.mall.dto.order;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data public class PaymentCallbackDTO { @NotBlank private String paymentNo; @NotBlank private String orderNo; @NotBlank private String amount; @NotBlank private String timestamp; @NotBlank private String signature; }
