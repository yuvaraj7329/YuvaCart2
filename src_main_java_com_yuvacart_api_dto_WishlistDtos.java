package com.yuvacart.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class WishlistDtos {
  private WishlistDtos() {}

  public record WishlistToggleRequest(
      @NotNull(message = "Product ID is required")
      Integer productId
  ) {}

  public record WishlistResponse(
      List<ProductDtos.ProductResponse> items,
      Integer count
  ) {}
}
