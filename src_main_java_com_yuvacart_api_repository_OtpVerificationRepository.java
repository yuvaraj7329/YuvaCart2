package com.yuvacart.api.repository;

import com.yuvacart.api.domain.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
  Optional<OtpVerification> findTopByPhoneNumberAndConsumedFalseOrderByCreatedAtDesc(String phoneNumber);
  List<OtpVerification> findByPhoneNumberAndConsumedFalse(String phoneNumber);
  long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant after);
}
