package com.yuvacart.api.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_variants")
public class ProductVariant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "product_id", nullable = false)
  private Integer productId;

  @Column(nullable = false, length = 120)
  private String label;

  @Column(nullable = false, unique = true, length = 80)
  private String sku;

  @Column(length = 60)
  private String color;

  @Column(length = 60)
  private String storage;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private Integer stock = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ProductVariant() {}

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
  public Integer getProductId() { return productId; }
  public void setProductId(Integer productId) { this.productId = productId; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public String getColor() { return color; }
  public void setColor(String color) { this.color = color; }
  public String getStorage() { return storage; }
  public void setStorage(String storage) { this.storage = storage; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
