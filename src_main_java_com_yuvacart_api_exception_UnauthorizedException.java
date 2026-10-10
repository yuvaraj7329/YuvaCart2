package com.yuvacart.api.exception;

public class UnauthorizedException extends ApiException {
  public UnauthorizedException(String message) {
    super(message, 401);
  }
}
