package com.yuvacart.api.service;

import com.yuvacart.api.exception.SmsConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class SmsService {
  private static final Logger log = LoggerFactory.getLogger(SmsService.class);

  @Value("${app.sms.provider:twilio}")
  private String provider;

  @Value("${app.sms.dev-mode:true}")
  private boolean devMode;

  @Value("${app.sms.twilio.account-sid:}")
  private String twilioAccountSid;

  @Value("${app.sms.twilio.auth-token:}")
  private String twilioAuthToken;

  @Value("${app.sms.twilio.from-number:}")
  private String twilioFromNumber;

  @Value("${app.sms.twilio.messaging-service-sid:}")
  private String twilioMessagingServiceSid;

  @Value("${app.sms.gateway.api-key:}")
  private String gatewayApiKey;

  @Value("${app.sms.gateway.sender-id:YUVACART}")
  private String gatewaySenderId;

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  /**
   * Sends an OTP SMS to the normalized phone number.
   *
   * @param phoneNumber E.164 formatted phone number (+919876543210)
   * @param otp Plain text 6-digit OTP (used only for outbound dispatch, never logged in production)
   */
  public void sendOtpSms(String phoneNumber, String otp) {
    String maskedPhone = PhoneUtils.mask(phoneNumber);
    String message = "Your YuvaCart verification code is " + otp + ". Valid for 5 minutes. Do not share this code.";

    if ("twilio".equalsIgnoreCase(provider)) {
      sendViaTwilio(phoneNumber, message, maskedPhone);
    } else if ("fast2sms".equalsIgnoreCase(provider) || "indian_gateway".equalsIgnoreCase(provider)) {
      sendViaIndianGateway(phoneNumber, otp, message, maskedPhone);
    } else {
      if (devMode) {
        log.info("[SMS MOCK/DEV] Dispatched OTP to {} (Provider: mock/dev)", maskedPhone);
      } else {
        throw new SmsConfigurationException("Unsupported or unconfigured SMS provider: " + provider);
      }
    }
  }

  private void sendViaTwilio(String phoneNumber, String message, String maskedPhone) {
    if (twilioAccountSid == null || twilioAccountSid.isBlank() ||
        twilioAuthToken == null || twilioAuthToken.isBlank()) {
      if (devMode) {
        log.warn("[DEV MODE] Twilio credentials missing in environment (TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN). OTP delivery simulated for {}", maskedPhone);
        return;
      }
      throw new SmsConfigurationException("Twilio SMS credentials (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN) are not configured. Please set them in environment variables.");
    }

    try {
      String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid.trim() + "/Messages.json";
      StringBuilder formBody = new StringBuilder();
      formBody.append("To=").append(URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8));
      formBody.append("&Body=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));

      if (twilioMessagingServiceSid != null && !twilioMessagingServiceSid.isBlank()) {
        formBody.append("&MessagingServiceSid=").append(URLEncoder.encode(twilioMessagingServiceSid.trim(), StandardCharsets.UTF_8));
      } else if (twilioFromNumber != null && !twilioFromNumber.isBlank()) {
        formBody.append("&From=").append(URLEncoder.encode(twilioFromNumber.trim(), StandardCharsets.UTF_8));
      } else {
        throw new SmsConfigurationException("Neither TWILIO_FROM_NUMBER nor TWILIO_MESSAGING_SERVICE_SID is configured.");
      }

      String credentials = twilioAccountSid.trim() + ":" + twilioAuthToken.trim();
      String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", authHeader)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
          .timeout(Duration.ofSeconds(15))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        log.info("Twilio SMS sent successfully to {}", maskedPhone);
      } else {
        log.error("Twilio SMS dispatch failed with HTTP {}: {}", response.statusCode(), response.body());
        throw new SmsConfigurationException("Twilio SMS delivery failed: " + response.body());
      }
    } catch (SmsConfigurationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to deliver SMS via Twilio to {}: {}", maskedPhone, e.getMessage());
      throw new SmsConfigurationException("Failed to send SMS via Twilio: " + e.getMessage());
    }
  }

  private void sendViaIndianGateway(String phoneNumber, String otp, String message, String maskedPhone) {
    if (gatewayApiKey == null || gatewayApiKey.isBlank()) {
      if (devMode) {
        log.warn("[DEV MODE] Indian SMS gateway API key missing (SMS_GATEWAY_API_KEY). Simulated OTP for {}", maskedPhone);
        return;
      }
      throw new SmsConfigurationException("Indian SMS gateway credentials (SMS_GATEWAY_API_KEY) are not configured. Please set them in environment variables.");
    }

    try {
      // Strip +91 for domestic 10-digit delivery if gateway expects 10 digits
      String nationalNumber = phoneNumber.startsWith("+91") ? phoneNumber.substring(3) : phoneNumber.replace("+", "");
      String url = "https://www.fast2sms.com/dev/bulkV2?authorization=" + URLEncoder.encode(gatewayApiKey.trim(), StandardCharsets.UTF_8)
          + "&route=otp&variables_values=" + URLEncoder.encode(otp, StandardCharsets.UTF_8)
          + "&flash=0&numbers=" + URLEncoder.encode(nationalNumber, StandardCharsets.UTF_8);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .GET()
          .timeout(Duration.ofSeconds(15))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        log.info("SMS delivered successfully via Indian Gateway to {}", maskedPhone);
      } else {
        log.error("Indian SMS Gateway response failed: {}", response.body());
        throw new SmsConfigurationException("SMS gateway delivery failed: " + response.body());
      }
    } catch (SmsConfigurationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to deliver SMS via gateway: {}", e.getMessage());
      throw new SmsConfigurationException("Failed to send SMS via gateway: " + e.getMessage());
    }
  }
}
