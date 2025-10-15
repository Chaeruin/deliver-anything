package com.deliveranything.domain.payment.dto;

public record PaymentConfirmRequest(String paymentKey, String orderId, Long amount) {

}
