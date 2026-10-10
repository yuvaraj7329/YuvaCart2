package com.yuvacart.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_otp_phone", columnList = "phone_number"),
    @Index(name = "idx_otp_expires", columnList = "expires_at")
})
public class OtpVerification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "phone_number", nullable = false, length = 30)
  private String phoneNumber;

  @Column(name = "otp_hash", nullable = false, length = 255)
  private String otpHash;

  @Column(nullable = false, length = 20)
  private String purpose; // "LOGIN" or "REGISTER"

  @Column(nullable = false)
  private Integer attempts = 0;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "resend_available_at", nullable = false)
  private Instant resendAvailableAt;

  @Column(nullable = false)
  private Boolean consumed = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public OtpVerification() {}

  public OtpVerification(String phoneNumber, String otpHash, String purpose, Instant expiresAt, Instant resendAvailableAt) {
    this.phoneNumber = phoneNumber;
    this.otpHash = otpHash;
    this.purpose = purpose;
    this.expiresAt = expiresAt;
    this.resendAvailableAt = resendAvailableAt;
    this.attempts = 0;
    this.consumed = false;
  }

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getPhoneNumber() { return phoneNumber; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
  public String getOtpHash() { return otpHash; }
  public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
  public String getPurpose() { return purpose; }
  public void setPurpose(String purpose) { this.purpose = purpose; }
  public Integer getAttempts() { return attempts; }
  public void setAttempts(Integer attempts) { this.attempts = attempts; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getResendAvailableAt() { return resendAvailableAt; }
  public void setResendAvailableAt(Instant resendAvailableAt) { this.resendAvailableAt = resendAvailableAt; }
  public Boolean getConsumed() { return consumed; }
  public void setConsumed(Boolean consumed) { this.consumed = consumed; }
  public Instant getCreatedAt() { return createdAt; }
}
