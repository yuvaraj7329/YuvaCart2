package com.yuvacart.api.dto;

import com.yuvacart.api.domain.ProductVariant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {
  private CartDtos() {}

  public record CartItemRequest(
      @NotNull(message = "Product ID is required")
      Integer productId,
      Integer variantId,
      @NotNull(message = "Quantity is required")
      @Min(value = 1, message = "Quantity must be at least 1")
      @Max(value = 99, message = "Quantity cannot exceed 99")
      Integer quantity
  ) {}

  public record CartLineDto(
      Integer cartItemId,
      ProductDtos.ProductResponse product,
      ProductVariant variant,
      Integer quantity,
      BigDecimal unitPrice,
      BigDecimal lineTotal
  ) {}

  public record CartResponse(
      List<CartLineDto> items,
      BigDecimal subtotal,
      Integer count,
      String currency
  ) {}
}
