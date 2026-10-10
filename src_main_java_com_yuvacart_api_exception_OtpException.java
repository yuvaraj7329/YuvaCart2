package com.yuvacart.api.exception;

public class OtpException extends ApiException {
  public OtpException(String message, int status) {
    super(message, status);
  }

  public OtpException(String message) {
    super(message, 400);
  }
}
