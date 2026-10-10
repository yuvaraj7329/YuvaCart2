package com.yuvacart.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
  private OrderDtos() {}

  public record CheckoutRequest(
      @NotBlank(message = "Recipient name is required")
      @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
      String shippingName,

      @NotBlank(message = "Phone number is required")
      @Size(min = 8, max = 30, message = "Valid phone number is required")
      String shippingPhone,

      @NotBlank(message = "Delivery address is required")
      @Size(min = 5, max = 240, message = "Address must be between 5 and 240 characters")
      String shippingAddress,

      @NotBlank(message = "City is required")
      @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
      String shippingCity,

      @NotBlank(message = "Postal PIN code is required")
      @Size(min = 4, max = 20, message = "Postal code must be between 4 and 20 characters")
      String shippingPostalCode,

      String paymentMethod // "COD" or "UPI" or "CARD"
  ) {}

  public record OrderItemDto(
      Integer id,
      Integer productId,
      String productName,
      Integer variantId,
      Integer quantity,
      BigDecimal unitPrice,
      BigDecimal lineTotal
  ) {}

  public record OrderResponse(
      Integer id,
      String orderNumber,
      BigDecimal totalAmount,
      String currency,
      String status,
      String shippingName,
      String shippingPhone,
      String shippingAddress,
      String shippingCity,
      String shippingPostalCode,
      String paymentMethod,
      Instant createdAt,
      List<OrderItemDto> items
  ) {}
}
