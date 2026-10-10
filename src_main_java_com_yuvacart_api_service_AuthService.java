package com.yuvacart.api.service;

import com.yuvacart.api.domain.OtpVerification;
import com.yuvacart.api.domain.User;
import com.yuvacart.api.dto.AuthDtos.*;
import com.yuvacart.api.exception.ApiException;
import com.yuvacart.api.exception.BadRequestException;
import com.yuvacart.api.exception.OtpException;
import com.yuvacart.api.exception.ResourceNotFoundException;
import com.yuvacart.api.exception.UnauthorizedException;
import com.yuvacart.api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final OtpService otpService;

  public AuthService(UserRepository userRepository, OtpService otpService) {
    this.userRepository = userRepository;
    this.otpService = otpService;
  }

  /**
   * Generates and dispatches OTP SMS. Checks registration status and validates rules.
   */
  @Transactional
  public SendOtpResponse sendOtp(SendOtpRequest request) {
    String normalized = PhoneUtils.normalize(request.phoneNumber());
    boolean isRegistered = userRepository.existsByPhoneNumber(normalized);
    String purpose = request.purpose() != null ? request.purpose().trim().toUpperCase() : "LOGIN";

    if ("LOGIN".equals(purpose) && !isRegistered) {
      return new SendOtpResponse(
          false,
          false,
          0,
          0,
          "This phone number is not registered with YuvaCart. Please create an account first."
      );
    }

    if ("REGISTER".equals(purpose) && isRegistered) {
      throw new BadRequestException("This phone number is already registered. Please sign in instead.");
    }

    OtpVerification challenge = otpService.generateAndSendOtp(normalized, purpose);
    long cooldown = Duration.between(Instant.now(), challenge.getResendAvailableAt()).toSeconds();
    long expiry = Duration.between(Instant.now(), challenge.getExpiresAt()).toSeconds();

    return new SendOtpResponse(
        true,
        isRegistered,
        Math.max(1, cooldown),
        Math.max(1, expiry),
        "A 6-digit verification code was sent to " + PhoneUtils.mask(normalized)
    );
  }

  /**
   * Verifies OTP, checks registered user in MySQL, establishes authenticated session.
   * Never allows login by submitting phone number alone!
   */
  @Transactional(noRollbackFor = {OtpException.class, ApiException.class})
  public AuthResponse verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    String normalized = PhoneUtils.normalize(request.phoneNumber());

    User user = userRepository.findByPhoneNumber(normalized)
        .orElseThrow(() -> new ResourceNotFoundException("Phone number not registered. Please register first."));

    // Verify OTP against hashed challenge
    otpService.verifyOtp(normalized, request.otp(), "LOGIN");

    // Create authenticated Spring Security session
    createAuthenticatedSession(user, httpRequest, httpResponse);

    return new AuthResponse(
        true,
        toUserResponse(user),
        "Welcome back, " + user.getUsername() + "!"
    );
  }

  /**
   * Registers a new user after verifying phone ownership with OTP.
   * Enforces unique phone numbers in MySQL.
   */
  @Transactional
  public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    String normalized = PhoneUtils.normalize(request.phoneNumber());

    if (userRepository.existsByPhoneNumber(normalized)) {
      throw new BadRequestException("An account already exists for this phone number. Please sign in.");
    }

    String cleanUsername = request.username().trim();
    if (cleanUsername.length() < 2 || cleanUsername.length() > 50) {
      throw new BadRequestException("Username must be between 2 and 50 characters.");
    }

    // Verify phone ownership with OTP
    otpService.verifyOtp(normalized, request.otp(), "REGISTER");

    // Save to MySQL
    User newUser = new User(cleanUsername, normalized);
    User saved = userRepository.save(newUser);

    // Create authenticated session
    createAuthenticatedSession(saved, httpRequest, httpResponse);

    return new AuthResponse(
        true,
        toUserResponse(saved),
        "Registration successful! Welcome to YuvaCart, " + saved.getUsername() + "."
    );
  }

  /**
   * Returns current authenticated user or throws 401 Unauthorized.
   */
  public UserResponse getCurrentUser(HttpServletRequest request) {
    SecurityContext context = SecurityContextHolder.getContext();
    if (context.getAuthentication() == null ||
        !context.getAuthentication().isAuthenticated() ||
        "anonymousUser".equals(context.getAuthentication().getPrincipal())) {
      throw new UnauthorizedException("Not authenticated");
    }

    Object principal = context.getAuthentication().getPrincipal();
    if (principal instanceof UserPrincipal userPrincipal) {
      User user = userRepository.findById(userPrincipal.id())
          .orElseThrow(() -> new UnauthorizedException("User session expired or not found."));
      return toUserResponse(user);
    }

    throw new UnauthorizedException("Invalid session");
  }

  /**
   * Invalidates authenticated session and clears security context.
   */
  public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
  }

  public User getRequiredAuthenticatedUser(HttpServletRequest request) {
    UserResponse currentUser = getCurrentUser(request);
    return userRepository.findById(currentUser.id())
        .orElseThrow(() -> new UnauthorizedException("User not found"));
  }

  private void createAuthenticatedSession(User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getPhoneNumber(), user.getRole());
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        principal,
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
    );

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    HttpSession session = httpRequest.getSession(true);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
  }

  private UserResponse toUserResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getPhoneNumber(),
        user.getRole()
    );
  }

  public record UserPrincipal(Integer id, String username, String phoneNumber, String role) {}
}
