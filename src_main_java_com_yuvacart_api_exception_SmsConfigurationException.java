package com.yuvacart.api.exception;

public class SmsConfigurationException extends ApiException {
  public SmsConfigurationException(String message) {
    super(message, 503);
  }
}
