package com.yuvacart.api.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user", columnList = "user_id"),
    @Index(name = "idx_orders_number", columnList = "order_number", unique = true)
})
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "order_number", nullable = false, unique = true, length = 64)
  private String orderNumber;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(nullable = false, length = 3)
  private String currency = "INR";

  @Column(nullable = false, length = 30)
  private String status = "CONFIRMED";

  @Column(name = "shipping_name", nullable = false, length = 120)
  private String shippingName;

  @Column(name = "shipping_phone", nullable = false, length = 30)
  private String shippingPhone;

  @Column(name = "shipping_address", nullable = false, length = 255)
  private String shippingAddress;

  @Column(name = "shipping_city", nullable = false, length = 100)
  private String shippingCity;

  @Column(name = "shipping_postal_code", nullable = false, length = 20)
  private String shippingPostalCode;

  @Column(name = "payment_method", length = 50)
  private String paymentMethod = "COD";

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<OrderItem> items = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Order() {}

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
  public String getOrderNumber() { return orderNumber; }
  public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getShippingName() { return shippingName; }
  public void setShippingName(String shippingName) { this.shippingName = shippingName; }
  public String getShippingPhone() { return shippingPhone; }
  public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }
  public String getShippingAddress() { return shippingAddress; }
  public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
  public String getShippingCity() { return shippingCity; }
  public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }
  public String getShippingPostalCode() { return shippingPostalCode; }
  public void setShippingPostalCode(String shippingPostalCode) { this.shippingPostalCode = shippingPostalCode; }
  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
  public List<OrderItem> getItems() { return items; }
  public void setItems(List<OrderItem> items) { this.items = items; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
