package com.deliveranything.domain.auth.verification.enums;

public enum VerificationType {
  EMAIL("이메일"),
  PHONE("휴대폰");

  private final String description;

  VerificationType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}