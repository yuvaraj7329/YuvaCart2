package com.yuvacart.api.exception;

public class ResourceNotFoundException extends ApiException {
  public ResourceNotFoundException(String message) {
    super(message, 404);
  }
}
