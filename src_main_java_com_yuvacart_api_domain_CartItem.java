package com.yuvacart.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cart_items")
public class CartItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "product_id", nullable = false)
  private Integer productId;

  @Column(name = "variant_id")
  private Integer variantId;

  @Column(nullable = false)
  private Integer quantity = 1;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public CartItem() {}

  public CartItem(Integer userId, Integer productId, Integer variantId, Integer quantity) {
    this.userId = userId;
    this.productId = productId;
    this.variantId = variantId;
    this.quantity = quantity;
  }

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
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getProductId() { return productId; }
  public void setProductId(Integer productId) { this.productId = productId; }
  public Integer getVariantId() { return variantId; }
  public void setVariantId(Integer variantId) { this.variantId = variantId; }
  public Integer getQuantity() { return quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
