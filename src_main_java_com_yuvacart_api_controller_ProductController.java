package com.yuvacart.api.controller;

import com.yuvacart.api.domain.Category;
import com.yuvacart.api.dto.ProductDtos.*;
import com.yuvacart.api.repository.CategoryRepository;
import com.yuvacart.api.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class ProductController {
  private final ProductService productService;
  private final CategoryRepository categoryRepository;

  public ProductController(ProductService productService, CategoryRepository categoryRepository) {
    this.productService = productService;
    this.categoryRepository = categoryRepository;
  }

  @GetMapping({"/api/products", "/api/v1/products"})
  public ResponseEntity<List<ProductResponse>> listProducts(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice) {
    return ResponseEntity.ok(productService.listProducts(new ProductFilter(query, category, minPrice, maxPrice)));
  }

  @GetMapping({"/api/products/{id:[0-9]+}", "/api/v1/products/{id:[0-9]+}"})
  public ResponseEntity<ProductDetailResponse> getProductById(@PathVariable Integer id) {
    return ResponseEntity.ok(productService.getProductById(id));
  }

  @GetMapping({"/api/products/{slug:[a-zA-Z0-9\\-]+}", "/api/v1/products/{slug:[a-zA-Z0-9\\-]+}"})
  public ResponseEntity<ProductDetailResponse> getProductBySlug(@PathVariable String slug) {
    return ResponseEntity.ok(productService.getProductBySlug(slug));
  }

  @GetMapping({"/api/categories", "/api/v1/categories"})
  public ResponseEntity<List<Category>> listCategories() {
    return ResponseEntity.ok(categoryRepository.findAll());
  }
}
