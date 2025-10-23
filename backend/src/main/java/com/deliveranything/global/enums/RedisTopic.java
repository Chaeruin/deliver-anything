package com.deliveranything.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisTopic {
  
  PAYMENT_EVENT("payment-*-event"),
  STOCK_EVENT("stock-*-event");

  private final String pattern;
}
