package com.deliveranything.domain.auth.verification.enums;

public enum VerificationPurpose {
  SIGNUP("회원가입"),
  LOGIN("로그인"),
  EMAIL_VERIFICATION("이메일 인증"),
  PASSWORD_RESET("비밀번호 재설정"),
  PROFILE_CHANGE("프로필 변경");

  private final String description;

  VerificationPurpose(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}