package com.deliveranything.domain.auth.verification.repository;

import com.deliveranything.domain.auth.verification.entity.VerificationToken;
import com.deliveranything.domain.auth.verification.enums.VerificationPurpose;
import com.deliveranything.domain.auth.verification.enums.VerificationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

  Optional<VerificationToken> findTopByIdentifierAndVerificationTypeAndPurposeOrderByCreatedAtDesc(
      String identifier,
      VerificationType verificationType,
      VerificationPurpose purpose
  );
}