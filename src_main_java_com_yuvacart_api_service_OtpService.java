package com.yuvacart.api.service;

import com.yuvacart.api.domain.OtpVerification;
import com.yuvacart.api.exception.ApiException;
import com.yuvacart.api.exception.BadRequestException;
import com.yuvacart.api.exception.OtpException;
import com.yuvacart.api.repository.OtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class OtpService {
  private static final Logger log = LoggerFactory.getLogger(OtpService.class);
  private final OtpVerificationRepository repository;
  private final SmsService smsService;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${app.otp.expiry-minutes:5}")
  private int expiryMinutes;

  @Value("${app.otp.cooldown-seconds:60}")
  private int cooldownSeconds;

  @Value("${app.otp.max-attempts:5}")
  private int maxAttempts;

  @Value("${app.sms.dev-mode:true}")
  private boolean devMode;

  public OtpService(OtpVerificationRepository repository, SmsService smsService) {
    this.repository = repository;
    this.smsService = smsService;
  }

  /**
   * Generates a real 6-digit OTP, checks cooldown & rate limits, invalidates previous OTPs,
   * stores SHA-256 hash, and dispatches via SMS.
   */
  @Transactional
  public OtpVerification generateAndSendOtp(String phoneNumber, String purpose) {
    String normalized = PhoneUtils.normalize(phoneNumber);

    // 1. Rate Limit: Check if in 60-second cooldown from latest active OTP
    Optional<OtpVerification> latestOpt = repository.findTopByPhoneNumberAndConsumedFalseOrderByCreatedAtDesc(normalized);
    Instant now = Instant.now();
    if (latestOpt.isPresent()) {
      OtpVerification latest = latestOpt.get();
      if (latest.getResendAvailableAt().isAfter(now)) {
        long remaining = Duration.between(now, latest.getResendAvailableAt()).toSeconds() + 1;
        throw new OtpException("Please wait " + remaining + " seconds before requesting a new OTP.", 429);
      }
    }

    // 2. Rate Limit: Max 10 requests per hour to prevent abuse
    long recentRequests = repository.countByPhoneNumberAndCreatedAtAfter(normalized, now.minus(Duration.ofHours(1)));
    if (recentRequests >= 10) {
      throw new OtpException("Too many OTP requests for this phone number. Please try again later.", 429);
    }

    // 3. Invalidate old unconsumed OTPs for this phone number
    List<OtpVerification> activeOtps = repository.findByPhoneNumberAndConsumedFalse(normalized);
    for (OtpVerification active : activeOtps) {
      active.setConsumed(true);
    }
    repository.saveAll(activeOtps);

    // 4. Generate cryptographically random 6-digit OTP
    int codeInt = 100000 + secureRandom.nextInt(900000);
    String otp = String.valueOf(codeInt);
    String otpHash = hashOtp(otp);

    Instant expiresAt = now.plus(Duration.ofMinutes(expiryMinutes));
    Instant resendAvailableAt = now.plus(Duration.ofSeconds(cooldownSeconds));

    OtpVerification challenge = new OtpVerification(normalized, otpHash, purpose, expiresAt, resendAvailableAt);
    repository.save(challenge);

    // In dev mode, log a notice for development convenience without logging in production
    if (devMode) {
      log.info("[DEV OTP NOTICE] Generated OTP for {}: {} (Purpose: {})", PhoneUtils.mask(normalized), otp, purpose);
    }

    // 5. Send real SMS via gateway
    smsService.sendOtpSms(normalized, otp);

    return challenge;
  }

  /**
   * Validates OTP code against stored SHA-256 hash. Enforces 5-minute expiry, max 5 attempts,
   * and single-use invalidation.
   */
  @Transactional(noRollbackFor = {OtpException.class, ApiException.class})
  public boolean verifyOtp(String phoneNumber, String otp, String purpose) {
    String normalized = PhoneUtils.normalize(phoneNumber);
    if (otp == null || !otp.matches("^\\d{6}$")) {
      throw new BadRequestException("OTP must be exactly 6 digits.");
    }

    OtpVerification challenge = repository.findTopByPhoneNumberAndConsumedFalseOrderByCreatedAtDesc(normalized)
        .orElseThrow(() -> new OtpException("No active OTP request found for this phone number. Please request an OTP."));

    Instant now = Instant.now();
    if (challenge.getExpiresAt().isBefore(now)) {
      challenge.setConsumed(true);
      repository.save(challenge);
      throw new OtpException("The OTP has expired. Please request a new code.");
    }

    if (challenge.getAttempts() >= maxAttempts) {
      challenge.setConsumed(true);
      repository.save(challenge);
      throw new OtpException("Maximum verification attempts exceeded. Please request a new OTP.", 429);
    }

    String candidateHash = hashOtp(otp.trim());
    if (!MessageDigest.isEqual(candidateHash.getBytes(StandardCharsets.UTF_8), challenge.getOtpHash().getBytes(StandardCharsets.UTF_8))) {
      challenge.setAttempts(challenge.getAttempts() + 1);
      repository.save(challenge);
      int remaining = maxAttempts - challenge.getAttempts();
      throw new OtpException("Invalid OTP code. " + (remaining > 0 ? remaining + " attempt(s) remaining." : "Please request a new OTP."));
    }

    // Mark single-use challenge as consumed
    challenge.setConsumed(true);
    repository.save(challenge);
    return true;
  }

  public static String hashOtp(String otp) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(otp.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte b : digest) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  public int getCooldownSeconds() {
    return cooldownSeconds;
  }

  public int getExpiryMinutes() {
    return expiryMinutes;
  }
}
