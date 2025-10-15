package com.deliveranything.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 인증/인가 관련 오류
  TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH-401", "유효하지 않은 토큰입니다."),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-401", "만료된 토큰입니다."),
  TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH-401", "토큰이 제공되지 않았습니다."),
  REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH-401", "유효하지 않은 리프레시 토큰입니다."),
  REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-401", "만료된 리프레시 토큰입니다."),
  REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH-401",
      "리프레시 토큰이 쿠키에 존재하지 않습니다. 다시 로그인해주세요."),
  PROFILE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "AUTH-403", "해당 프로필로 사용할 권한이 없습니다."),
  PROFILE_SWITCH_FAILED(HttpStatus.BAD_REQUEST, "AUTH-400", "프로필 전환에 실패했습니다."),

  // 토큰 재발급 관련 오류
  TOKEN_REFRESH_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH-429",
      "토큰 재발급 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
  REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.FORBIDDEN, "AUTH-403",
      "보안 위협이 감지되었습니다. 모든 기기에서 로그아웃됩니다."),

  // 유저 관련 오류
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-404", "사용자를 찾을 수 없습니다."),
  USER_EMAIL_ALREADY_EXIST(HttpStatus.CONFLICT, "USER-409", "이미 존재하는 이메일 입니다."),
  USER_PHONE_ALREADY_EXIST(HttpStatus.CONFLICT, "USER-409", "이미 존재하는 핸드폰 번호 입니다."),

  // 프로필 관련 오류
  PROFILE_REQUIRED(HttpStatus.FORBIDDEN, "PROFILE-403", "프로필이 필요합니다. 프로필을 생성해주세요."),
  PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE-404", "요청한 프로필을 찾을 수 없습니다."),
  PROFILE_ALREADY_ACTIVE(HttpStatus.CONFLICT, "PROFILE-409", "이미 활성화된 프로필입니다."),
  PROFILE_INACTIVE(HttpStatus.FORBIDDEN, "PROFILE-403", "비활성화된 프로필로는 전환할 수 없습니다."),
  PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROFILE-409", "이미 해당 타입의 프로필이 존재합니다."),
  BUSINESS_CERTIFICATE_DUPLICATE(HttpStatus.CONFLICT, "SELLER-409", "이미 등록된 사업자등록번호입니다."),

  // 배달/라이더 관련 오류
  RIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "RIDER-404", "라이더를 찾을 수 없습니다."),
  DELIVERY_NOT_AVAILABLE_IN_PROGRESS(HttpStatus.CONFLICT, "DELIVERY-409",
      "현재 배달 진행 중이므로 해당 작업을 수행할 수 없습니다."),
  DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY-404", "배달을 찾을 수 없습니다."),
  NO_ACTIVE_DELIVERY(HttpStatus.NOT_FOUND, "DELIVERY-405", "진행 중인 배달이 없습니다."),

  // 리뷰 관련 오류
  REVIEW_NO_PERMISSION(HttpStatus.FORBIDDEN, "REVIEW-403", "리뷰를 관리할 권한이 없습니다."),
  REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW-404", "리뷰를 찾을 수 없습니다."),
  REVIEW_ALREADY_LIKED(HttpStatus.CONFLICT, "REVIEW-001", "리뷰에 이미 좋아요를 눌렀습니다."),
  REVIEW_NOT_LIKED(HttpStatus.CONFLICT, "REVIEW-002", "좋아요를 누르지 않은 리뷰입니다."),
  REVIEW_INVALID_TARGET(HttpStatus.BAD_REQUEST, "REVIEW-003", "리뷰 대상이 유효하지 않습니다."),

  // 주문 관련 오류
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-404", "주문 정보를 찾을 수 없습니다."),
  CUSTOMER_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-CUSTOMER-404", "소비자의 주문을 찾을 수 없습니다."),
  ORDER_PAY_UNAVAILABLE(HttpStatus.CONFLICT, "ORDER-409", "결제가 불가능한 주문입니다."),
  ORDER_CANCEL_UNAVAILABLE(HttpStatus.CONFLICT, "ORDER-410", "상점이 이미 준비중인 주문입니다."),

  // 결제 관련 오류
  PG_PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "PG-PAYMENT-001",
      "결제 검증 실패: 결제 번호, 주문 번호, 결제 금액 중 하나 이상 불일치합니다."),
  PG_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PG-PAYMENT-404", "PG사에서 주문의 결제 이력을 찾을 수 없습니다."),
  PG_PAYMENT_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PG-PAYMENT-CANCEL-500",
      "일시적인 서버 오류입니다. 계속 실패한다면 관리자에게 문의 바랍니다."),

  PAYMENT_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "PAYMENT-001", "결제 금액이 주문의 가격과 다릅니다."),
  PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT-404", "결제 이력을 찾을 수 없습니다."),
  PAYMENT_INVALID_STATUS(HttpStatus.CONFLICT, "PAYMENT-409", "결제 상태를 변경할 수 없습니다."),

  // 상점 관련 오류
  STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE-404", "상점을 찾을 수 없습니다."),
  STORE_ALREADY_EXISTS(HttpStatus.CONFLICT, "STORE-409", "이미 해당 판매자 프로필로 생성된 상점이 존재합니다."),
  STORE_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE-CATEGORY-404", "상점 카테고리를 찾을 수 없습니다."),
  STORE_NOT_READY_FOR_OPENING(HttpStatus.BAD_REQUEST, "STORE-400", "상점이 아직 모든 정보가 입력된 상태가 아닙니다."),
  STORE_OWNER_MISMATCH(HttpStatus.FORBIDDEN, "STORE-403", "요청 사용자는 해당 상점의 주인이 아닙니다."),

  // 상품, 재고 관련 오류
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-404", "상품을 찾을 수 없습니다."),
  PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT-400", "상품의 재고가 부족합니다."),
  PRODUCT_STORE_MISMATCH(HttpStatus.BAD_REQUEST, "PRODUCT-400", "해당 상품은 지정된 상점에 속하지 않습니다."),
  STOCK_CHANGE_INVALID(HttpStatus.BAD_REQUEST, "STOCK-400", "재고 변경 수량이 유효하지 않습니다."),
  STOCK_CHANGE_CONFLICT(HttpStatus.CONFLICT, "STOCK-409", "재고 변경 충돌이 발생했습니다. 다시 시도해주세요."),
  STORE_CLOSED(HttpStatus.BAD_REQUEST, "STORE-400", "상점이 현재 닫혀 있어 주문을 처리할 수 없습니다."),

  // 정산 관련 오류
  SETTLEMENT_BATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTLEMENT-BATCH-404", "정산 정보를 찾을 수 없습니다."),
  SETTLEMENT_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTLEMENT-DETAIL-404",
      "세부 정산 정보를 찾을 수 없습니다."),

  // ------- 공통 오류 ---------

  // SSE 관련 오류
  SSE_SUBSCRIBE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SSE-503", "SSE 연결에 실패했습니다."),

  // Redis 관련 오류
  REDIS_MESSAGE_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS-500",
      "Redis 메시지 처리 중 오류가 발생했습니다."),

  // WebClient 관련 오류
  WEBCLIENT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "WEBCLIENT-503", "외부 서비스 요청에 실패했습니다."),
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}