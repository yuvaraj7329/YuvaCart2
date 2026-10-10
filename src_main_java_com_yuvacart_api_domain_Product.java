package com.yuvacart.api.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_category", columnList = "category"),
    @Index(name = "idx_products_featured", columnList = "isFeatured"),
    @Index(name = "idx_products_active", columnList = "active")
})
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(nullable = false, unique = true, length = 220)
  private String slug;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(precision = 5, scale = 2)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(name = "compare_at_price", precision = 12, scale = 2)
  private BigDecimal compareAtPrice;

  @Column(name = "stock_quantity", nullable = false)
  private Integer stockQuantity = 0;

  @Column(name = "image_url", nullable = false, length = 500)
  private String imageUrl;

  @Column(name = "category_id")
  private Integer categoryId;

  @Column(nullable = false, length = 80)
  private String category;

  @Column(nullable = false, length = 3)
  private String currency = "INR";

  @Column(name = "seller_name", nullable = false, length = 120)
  private String sellerName = "YuvaCart Direct";

  @Column(nullable = false, precision = 3, scale = 2)
  private BigDecimal rating = new BigDecimal("4.50");

  @Column(name = "review_count", nullable = false)
  private Integer reviewCount = 0;

  @Column(name = "isFeatured", nullable = false)
  private Integer isFeatured = 0;

  @Column(columnDefinition = "text")
  private String specifications;

  @Column(nullable = false)
  private Boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Product() {}

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getSlug() { return slug; }
  public void setSlug(String slug) { this.slug = slug; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public BigDecimal getDiscount() { return discount; }
  public void setDiscount(BigDecimal discount) { this.discount = discount; }
  public BigDecimal getCompareAtPrice() { return compareAtPrice; }
  public void setCompareAtPrice(BigDecimal compareAtPrice) { this.compareAtPrice = compareAtPrice; }
  public Integer getStockQuantity() { return stockQuantity; }
  public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  public Integer getCategoryId() { return categoryId; }
  public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public String getSellerName() { return sellerName; }
  public void setSellerName(String sellerName) { this.sellerName = sellerName; }
  public BigDecimal getRating() { return rating; }
  public void setRating(BigDecimal rating) { this.rating = rating; }
  public Integer getReviewCount() { return reviewCount; }
  public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
  public Integer getIsFeatured() { return isFeatured; }
  public void setIsFeatured(Integer isFeatured) { this.isFeatured = isFeatured; }
  public String getSpecifications() { return specifications; }
  public void setSpecifications(String specifications) { this.specifications = specifications; }
  public Boolean getActive() { return active; }
  public void setActive(Boolean active) { this.active = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
