package com.deliveranything.domain.auth.verification.entity;

import com.deliveranything.domain.auth.verification.enums.VerificationPurpose;
import com.deliveranything.domain.auth.verification.enums.VerificationType;
import com.deliveranything.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "verification_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationToken extends BaseEntity {

  @Column(name = "identifier", nullable = false, columnDefinition = "VARCHAR(255)")
  private String identifier; // 이메일 또는 휴대폰번호

  @Column(name = "verification_code", nullable = false, columnDefinition = "VARCHAR(10)")
  private String verificationCode; // 6자리 인증번호

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_type", nullable = false, columnDefinition = "VARCHAR(20)")
  private VerificationType verificationType; // EMAIL, PHONE

  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false, columnDefinition = "VARCHAR(30)")
  private VerificationPurpose purpose; // SIGNUP, LOGIN, PASSWORD_RESET 등

  @Column(name = "expire_at", nullable = false)
  private LocalDateTime expireAt;

  @Column(name = "is_used", nullable = false)
  private boolean isUsed;

  @Builder
  public VerificationToken(String identifier, VerificationType verificationType,
      VerificationPurpose purpose, int validMinutes) {
    this.identifier = identifier;
    this.verificationCode = generateVerificationCode();
    this.verificationType = verificationType;
    this.purpose = purpose;
    this.expireAt = LocalDateTime.now().plusMinutes(validMinutes);
    this.isUsed = false;
  }

  // 비즈니스 메서드

  // 6자리 랜덤 인증번호 생성

  private String generateVerificationCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000); // 100000~999999
    return String.valueOf(code);
  }

  // 인증번호가 만료되었는지 확인
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expireAt);
  }

  // 인증번호가 유효한지 확인 (사용되지 않았고 만료되지 않음)
  public boolean isValid() {
    return !isUsed && !isExpired();
  }

  // 입력된 코드가 올바른지 검증
  public boolean verifyCode(String inputCode) {
    if (!isValid()) {
      return false;
    }
    return this.verificationCode.equals(inputCode);
  }

  // 인증번호 사용 처리

  public void markAsUsed() {
    this.isUsed = true;
  }

  // 특정 식별자(이메일/휴대폰)에 대한 토큰인지 확인

  public boolean belongsToIdentifier(String identifier) {
    return this.identifier.equals(identifier);
  }

  // 특정 목적의 토큰인지 확인

  public boolean isForPurpose(VerificationPurpose purpose) {
    return this.purpose == purpose;
  }

  // 만료 시간 연장 (재발송 시 사용)

  public void extendExpiration(int additionalMinutes) {
    this.expireAt = LocalDateTime.now().plusMinutes(additionalMinutes);
  }

  // 새로운 인증번호로 갱신 (재발송 시 사용)

  public void regenerateCode() {
    this.verificationCode = generateVerificationCode();
    this.isUsed = false; // 재생성하면 다시 사용 가능하게
  }
}