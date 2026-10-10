package com.yuvacart.api.service;

import com.yuvacart.api.domain.Product;
import com.yuvacart.api.dto.ProductDtos.*;
import com.yuvacart.api.exception.ResourceNotFoundException;
import com.yuvacart.api.repository.ProductRepository;
import com.yuvacart.api.repository.ProductVariantRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ProductService {
  private static final Set<String> EXCLUDED_PRODUCT_NAMES = Set.of(
      "VoltGo 20,000 mAh Power Bank",
      "Botanical Care Starter Set"
  );

  private final ProductRepository productRepository;
  private final ProductVariantRepository variantRepository;

  public ProductService(ProductRepository productRepository, ProductVariantRepository variantRepository) {
    this.productRepository = productRepository;
    this.variantRepository = variantRepository;
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> listProducts(ProductFilter filter) {
    Specification<Product> spec = (root, query, cb) -> cb.isTrue(root.get("active"));

    // Exclude VoltGo and Botanical Care strictly from customer-facing listings
    spec = spec.and((root, query, cb) -> cb.not(root.get("name").in(EXCLUDED_PRODUCT_NAMES)));

    if (filter != null) {
      if (filter.category() != null && !filter.category().isBlank() && !"All".equalsIgnoreCase(filter.category().trim())) {
        spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("category")), filter.category().trim().toLowerCase()));
      }

      if (filter.query() != null && !filter.query().isBlank()) {
        String pattern = "%" + filter.query().trim().toLowerCase() + "%";
        spec = spec.and((root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("name")), pattern),
            cb.like(cb.lower(root.get("description")), pattern),
            cb.like(cb.lower(root.get("category")), pattern),
            cb.like(cb.lower(root.get("sellerName")), pattern)
        ));
      }

      if (filter.minPrice() != null) {
        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
      }

      if (filter.maxPrice() != null) {
        spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
      }
    }

    Sort sort = Sort.by(Sort.Order.desc("isFeatured"), Sort.Order.desc("id"));
    return productRepository.findAll(spec, sort).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public ProductDetailResponse getProductBySlug(String slug) {
    Product product = productRepository.findBySlug(slug)
        .filter(p -> p.getActive() && !EXCLUDED_PRODUCT_NAMES.contains(p.getName()))
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));

    return new ProductDetailResponse(
        toResponse(product),
        variantRepository.findByProductIdOrderByPriceAsc(product.getId())
    );
  }

  @Transactional(readOnly = true)
  public ProductDetailResponse getProductById(Integer id) {
    Product product = productRepository.findById(id)
        .filter(p -> p.getActive() && !EXCLUDED_PRODUCT_NAMES.contains(p.getName()))
        .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

    return new ProductDetailResponse(
        toResponse(product),
        variantRepository.findByProductIdOrderByPriceAsc(product.getId())
    );
  }

  public ProductResponse toResponse(Product p) {
    return new ProductResponse(
        p.getId(),
        p.getName(),
        p.getSlug(),
        p.getDescription(),
        p.getPrice(),
        p.getDiscount(),
        p.getCompareAtPrice(),
        p.getStockQuantity(),
        p.getImageUrl(),
        p.getCategoryId(),
        p.getCategory(),
        p.getCurrency(),
        p.getSellerName(),
        p.getRating(),
        p.getReviewCount(),
        p.getIsFeatured(),
        p.getSpecifications(),
        p.getActive()
    );
  }
}
