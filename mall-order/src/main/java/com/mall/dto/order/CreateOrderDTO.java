package com.mall.dto.order;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import lombok.Data; import java.util.List;
@Data public class CreateOrderDTO { @NotNull private Long addressId; private Long couponId; private String remark; @NotEmpty @Valid private List<OrderItemDTO> items; @Data public static class OrderItemDTO { @NotNull private Long skuId; @NotNull private Long productId; @NotNull @Positive private Integer quantity; } }
