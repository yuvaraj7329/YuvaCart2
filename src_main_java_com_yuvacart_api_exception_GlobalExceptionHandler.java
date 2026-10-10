package com.yuvacart.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("success", false);
    body.put("error", ex.getMessage());
    body.put("status", ex.getStatus());
    body.put("timestamp", Instant.now().toString());
    return ResponseEntity.status(ex.getStatus()).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    Map<String, Object> body = new HashMap<>();
    body.put("success", false);
    body.put("error", "Validation failed");
    body.put("details", errors);
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("timestamp", Instant.now().toString());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("success", false);
    body.put("error", ex.getMessage());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("timestamp", Instant.now().toString());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("success", false);
    body.put("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred.");
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("timestamp", Instant.now().toString());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
