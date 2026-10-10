package com.yuvacart.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wishlist", uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_product", columnNames = {"user_id", "product_id"})
})
public class WishlistItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "product_id", nullable = false)
  private Integer productId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public WishlistItem() {}

  public WishlistItem(Integer userId, Integer productId) {
    this.userId = userId;
    this.productId = productId;
  }

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getProductId() { return productId; }
  public void setProductId(Integer productId) { this.productId = productId; }
  public Instant getCreatedAt() { return createdAt; }
}
