package com.yuvacart.api.service;

import com.yuvacart.api.exception.BadRequestException;

public final class PhoneUtils {
  private PhoneUtils() {}

  /**
   * Normalizes a phone number to standard international format (E.164).
   * E.g., "9876543210" -> "+919876543210"
   * "+91 98765-43210" -> "+919876543210"
   */
  public static String normalize(String rawPhone) {
    if (rawPhone == null || rawPhone.isBlank()) {
      throw new BadRequestException("Phone number is required");
    }
    String cleaned = rawPhone.trim().replaceAll("[\\s\\-\\(\\)]", "");
    if (!cleaned.startsWith("+")) {
      // Default to Indian country code +91 if not specified
      if (cleaned.length() == 10 && cleaned.matches("^\\d{10}$")) {
        cleaned = "+91" + cleaned;
      } else if (cleaned.length() == 12 && cleaned.startsWith("91")) {
        cleaned = "+" + cleaned;
      } else {
        cleaned = "+" + cleaned;
      }
    }
    if (!cleaned.matches("^\\+[1-9]\\d{7,14}$")) {
      throw new BadRequestException("Invalid phone number format: " + rawPhone + ". Must be valid with country code (e.g. +919876543210).");
    }
    return cleaned;
  }

  /**
   * Masks a phone number for secure display, e.g. "+919876543210" -> "+91••••••3210"
   */
  public static String mask(String phone) {
    if (phone == null || phone.length() < 7) {
      return phone != null ? phone : "";
    }
    int prefixLen = Math.min(3, phone.length() - 4);
    String prefix = phone.substring(0, prefixLen);
    String suffix = phone.substring(phone.length() - 4);
    int maskLen = Math.max(2, phone.length() - prefixLen - 4);
    return prefix + "•".repeat(maskLen) + suffix;
  }
}
