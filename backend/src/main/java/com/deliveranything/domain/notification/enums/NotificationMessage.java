package com.deliveranything.domain.notification.enums;

import com.deliveranything.domain.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NotificationMessage {

  ORDER_CREATED_CUSTOMER("주문번호가 생성되었습니다. 결제를 진행해주세요."),
  ORDER_CREATED_FAILED_CUSTOMER("재고가 다 떨어졌습니다. 다시 주문해주세요."),
  ORDER_PAID_CUSTOMER("주문 결제가 성공적으로 완료되었습니다."),
  ORDER_PAID_SELLER("새로운 주문 요청이 있습니다."),
  ORDER_PREPARING_CUSTOMER("주문이 준비중입니다."),
  ORDER_PREPARING_SELLER("주문이 수락되어 주문을 준비할 수 있습니다."),
  ORDER_PAYMENT_FAILED_CUSTOMER("결제에 실패했습니다. 장바구니에서 다시 결제해주세요."),
  ORDER_CANCELED_CUSTOMER("주문이 성공적으로 취소되었습니다."),
  ORDER_CANCELED_SELLER("주문이 정상적으로 거절되었습니다."),
  ORDER_CANCEL_FAILED_CUSTOMER("주문 취소되었고 3일 내에 계좌에 환불됩니다."),
  // 상점의 거절 or 고객의 거절처리 중 어떤 결과(성공, 실패)든 무조건 취소됨
  ORDER_CANCEL_FAILED_SELLER("주문이 정상적으로 취소되었습니다."),

  RIDER_STATUS_CHANGED("배달원 상태가 변경되었습니다."),
  RIDER_ACCEPTED_ORDER("수락된 주문이 표시됩니다."),
  RIDER_DECISION("배달원이 주문 상태를 변경했습니다."),
  ORDER_ASSIGN_FAILED("배달원 배정에 실패했습니다. 다시 시도해주세요."),
  ;

  private final String message;

  public static String getMessageByOrderStatus(OrderStatus orderStatus) {
    return switch (orderStatus) {
      case OrderStatus.RIDER_ASSIGNED -> "배달원이 배정됐습니다.";
      case OrderStatus.DELIVERING -> "음식이 배달중입니다.";
      case OrderStatus.COMPLETED -> "배달이 완료되었습니다.";
      default -> "정의하지 않은 SSE 주문 상태 알림입니다. -> " + orderStatus;
    };
  }
}
