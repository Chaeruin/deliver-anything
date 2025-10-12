package com.deliveranything.domain.notification.enums;

// 예시 입니다 - 필요에 따라 알림 유형을 추가하거나 수정하세요.
public enum NotificationType {
  NEW_ORDER,          // 판매자에게 새 주문 알림
  ORDER_ACCEPTED,     // 소비자에게 주문 수락 알림
  ORDER_REJECTED,     // 소비자에게 주문 거절 알림
  NEW_REVIEW,         // 판매자 or 배달원에게 새 리뷰 알림
  ORDER_PAID_CUSTOMER,
  ORDER_PAID_SELLER,
  ORDER_PREPARING_CUSTOMER,
  ORDER_PREPARING_SELLER,
  ORDER_PAYMENT_FAILED_CUSTOMER,
  ORDER_CANCELED_CUSTOMER,
  ORDER_CANCELED_SELLER,
  ORDER_CANCEL_FAILED_CUSTOMER,
  ORDER_CANCEL_FAILED_SELLER,
  ORDER_STATUS_CHANGED_CUSTOMER,
  ORDER_STATUS_CHANGED_SELLER,
  RIDER_STATUS_CHANGED,
  RIDER_ACCEPTED_ORDER,
  RIDER_DECISION
}