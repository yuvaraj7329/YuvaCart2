package com.yuvacart.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  @JsonIgnore
  private Order order;

  @Column(name = "product_id", nullable = false)
  private Integer productId;

  @Column(name = "product_name", nullable = false, length = 180)
  private String productName;

  @Column(name = "variant_id")
  private Integer variantId;

  @Column(nullable = false)
  private Integer quantity = 1;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal lineTotal;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public OrderItem() {}

  public OrderItem(Order order, Integer productId, String productName, Integer variantId, Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    this.order = order;
    this.productId = productId;
    this.productName = productName;
    this.variantId = variantId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.lineTotal = lineTotal;
  }

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Order getOrder() { return order; }
  public void setOrder(Order order) { this.order = order; }
  public Integer getProductId() { return productId; }
  public void setProductId(Integer productId) { this.productId = productId; }
  public String getProductName() { return productName; }
  public void setProductName(String productName) { this.productName = productName; }
  public Integer getVariantId() { return variantId; }
  public void setVariantId(Integer variantId) { this.variantId = variantId; }
  public Integer getQuantity() { return quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
  public BigDecimal getLineTotal() { return lineTotal; }
  public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
  public Instant getCreatedAt() { return createdAt; }
}
