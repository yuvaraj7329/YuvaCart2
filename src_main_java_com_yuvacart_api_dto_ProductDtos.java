package com.yuvacart.api.dto;

import com.yuvacart.api.domain.ProductVariant;
import java.math.BigDecimal;
import java.util.List;

public final class ProductDtos {
  private ProductDtos() {}

  public record ProductResponse(
      Integer id,
      String name,
      String slug,
      String description,
      BigDecimal price,
      BigDecimal discount,
      BigDecimal compareAtPrice,
      Integer stockQuantity,
      String imageUrl,
      Integer categoryId,
      String category,
      String currency,
      String sellerName,
      BigDecimal rating,
      Integer reviewCount,
      Integer isFeatured,
      String specifications,
      Boolean active
  ) {}

  public record ProductDetailResponse(
      ProductResponse product,
      List<ProductVariant> variants
  ) {}

  public record ProductFilter(
      String query,
      String category,
      BigDecimal minPrice,
      BigDecimal maxPrice
  ) {}
}
