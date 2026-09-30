package vn.tnteco.demo.data.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotNull(message = "userId không được để trống")
        Long userId,

        @NotNull(message = "productId không được để trống")
        Long productId,

        @NotNull(message = "quantity không được để trống")
        @Positive(message = "quantity phải lớn hơn 0")
        Integer quantity
) {
}
