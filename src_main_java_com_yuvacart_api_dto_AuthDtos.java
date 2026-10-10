package com.yuvacart.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
  private AuthDtos() {}

  public record SendOtpRequest(
      @NotBlank(message = "Phone number is required")
      @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Valid phone number with country code is required (e.g. +919876543210)")
      String phoneNumber,

      String purpose // "LOGIN" or "REGISTER"
  ) {}

  public record SendOtpResponse(
      boolean success,
      boolean registered,
      long cooldownSeconds,
      long expiresInSeconds,
      String message
  ) {}

  public record VerifyOtpRequest(
      @NotBlank(message = "Phone number is required")
      @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Valid phone number with country code is required")
      String phoneNumber,

      @NotBlank(message = "OTP is required")
      @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
      String otp
  ) {}

  public record RegisterRequest(
      @NotBlank(message = "Username is required")
      @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
      String username,

      @NotBlank(message = "Phone number is required")
      @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Valid phone number with country code is required")
      String phoneNumber,

      @NotBlank(message = "OTP is required")
      @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
      String otp
  ) {}

  public record UserResponse(
      Integer id,
      String username,
      String phoneNumber,
      String role
  ) {}

  public record AuthResponse(
      boolean success,
      UserResponse user,
      String message
  ) {}

  public record MessageResponse(
      boolean success,
      String message
  ) {}
}
