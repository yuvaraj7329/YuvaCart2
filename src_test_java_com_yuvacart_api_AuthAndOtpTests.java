package com.yuvacart.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuvacart.api.domain.OtpVerification;
import com.yuvacart.api.domain.User;
import com.yuvacart.api.dto.AuthDtos.RegisterRequest;
import com.yuvacart.api.dto.AuthDtos.SendOtpRequest;
import com.yuvacart.api.dto.AuthDtos.VerifyOtpRequest;
import com.yuvacart.api.repository.OtpVerificationRepository;
import com.yuvacart.api.repository.UserRepository;
import com.yuvacart.api.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthAndOtpTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private OtpVerificationRepository otpRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    otpRepository.deleteAll();
    userRepository.deleteAll();

    // Create an existing test user
    User user = new User("Yuvaraj", "+919876543210");
    userRepository.save(user);
  }

  @Test
  @DisplayName("POST /api/auth/send-otp - login with unregistered phone returns registered: false")
  void testSendOtpUnregisteredPhone() throws Exception {
    SendOtpRequest request = new SendOtpRequest("+919999988888", "LOGIN");

    mockMvc.perform(post("/api/auth/send-otp")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.registered").value(false))
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("POST /api/auth/send-otp - registered user receives OTP with cooldown")
  void testSendOtpRegisteredUser() throws Exception {
    SendOtpRequest request = new SendOtpRequest("+919876543210", "LOGIN");

    mockMvc.perform(post("/api/auth/send-otp")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.registered").value(true))
        .andExpect(jsonPath("$.cooldownSeconds").isNumber())
        .andExpect(jsonPath("$.expiresInSeconds").isNumber());

    // Verify OTP challenge was persisted in DB
    assertTrue(otpRepository.findTopByPhoneNumberAndConsumedFalseOrderByCreatedAtDesc("+919876543210").isPresent());
  }

  @Test
  @DisplayName("POST /api/auth/verify-otp - incorrect OTP increments attempt and is rejected")
  void testVerifyOtpIncorrect() throws Exception {
    // Generate valid OTP
    SendOtpRequest sendReq = new SendOtpRequest("+919876543210", "LOGIN");
    mockMvc.perform(post("/api/auth/send-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(sendReq)));

    // Verify with incorrect code
    VerifyOtpRequest verifyReq = new VerifyOtpRequest("+919876543210", "000000");
    mockMvc.perform(post("/api/auth/verify-otp")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(verifyReq)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());

    // Check attempts increased
    OtpVerification challenge = otpRepository.findTopByPhoneNumberAndConsumedFalseOrderByCreatedAtDesc("+919876543210").orElseThrow();
    assertEquals(1, challenge.getAttempts());
  }

  @Test
  @DisplayName("POST /api/auth/register and duplicate phone rejection")
  void testRegisterAndDuplicatePhoneRejection() throws Exception {
    String newPhone = "+919123456789";

    // 1. Request OTP for registration
    SendOtpRequest sendReq = new SendOtpRequest(newPhone, "REGISTER");
    mockMvc.perform(post("/api/auth/send-otp")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(sendReq)))
        .andExpect(status().isOk());

    // Directly set known OTP in database for testing
    OtpVerification challenge = otpRepository.findTopByPhoneNumberAndConsumedFalseOrderByCreatedAtDesc(newPhone).orElseThrow();
    challenge.setOtpHash(OtpService.hashOtp("123456"));
    otpRepository.save(challenge);

    // 2. Complete registration
    RegisterRequest registerReq = new RegisterRequest("NewUser", newPhone, "123456");
    MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.user.username").value("NewUser"))
        .andExpect(jsonPath("$.user.phoneNumber").value(newPhone))
        .andReturn();

    MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
    assertNotNull(session);

    // 3. Verify session restoration via GET /api/auth/me
    mockMvc.perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("NewUser"));

    // 4. Try duplicate registration with same phone number
    mockMvc.perform(post("/api/auth/send-otp")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new SendOtpRequest(newPhone, "REGISTER"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("This phone number is already registered. Please sign in instead."));
  }

  @Test
  @DisplayName("Verify unauthenticated access to private APIs is blocked (401)")
  void testPrivateApiProtection() throws Exception {
    mockMvc.perform(get("/api/cart"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/wishlist"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/orders"))
        .andExpect(status().isUnauthorized());
  }
}
