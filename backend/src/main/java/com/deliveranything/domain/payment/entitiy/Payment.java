package com.deliveranything.domain.payment.entitiy;

import com.deliveranything.domain.payment.enums.PaymentStatus;
import com.deliveranything.global.entity.BaseEntity;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

  @Column(nullable = false, unique = true, length = 64)
  private String merchantUid;

  @Column(unique = true, length = 200)
  private String paymentKey;

  @Column(nullable = false, precision = 19, scale = 2)
  private Long amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  public Payment(String merchantUid, String paymentKey, Long amount, PaymentStatus status) {
    this.merchantUid = merchantUid;
    this.paymentKey = paymentKey;
    this.amount = amount;
    this.status = status;
  }

  public void updateStatus(PaymentStatus status) {
    if (this.status != PaymentStatus.READY) {
      throw new CustomException(ErrorCode.PAYMENT_INVALID_STATUS);
    }

    this.status = status;
  }
}
