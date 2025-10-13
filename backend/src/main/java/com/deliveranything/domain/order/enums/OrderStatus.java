package com.deliveranything.domain.order.enums;

public enum OrderStatus {
  CREATED,
  PENDING,
  PREPARING,
  RIDER_ASSIGNED,
  DELIVERING,
  COMPLETED,
  REJECTED,
  CANCELED,
  CANCELLATION_REQUESTED,
  CANCEL_FAILED,
  PAYMENT_FAILED;

  public boolean canTransitTo(OrderStatus next) {
    return switch (this) {
      case CREATED -> next == PENDING || next == PAYMENT_FAILED || next == CANCELED;
      case PENDING -> next == PREPARING || next == REJECTED || next == CANCELED
          || next == CANCELLATION_REQUESTED;
      case PREPARING -> next == RIDER_ASSIGNED;
      case RIDER_ASSIGNED -> next == DELIVERING;
      case DELIVERING -> next == COMPLETED;
      case CANCELLATION_REQUESTED -> next == PENDING || next == CANCEL_FAILED;
      case COMPLETED, REJECTED, CANCELED, CANCEL_FAILED, PAYMENT_FAILED -> false;
    };
  }
}
