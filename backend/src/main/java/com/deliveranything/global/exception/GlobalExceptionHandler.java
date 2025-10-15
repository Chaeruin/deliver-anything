package com.deliveranything.global.exception;

import com.deliveranything.global.common.ApiResponse;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
    log.info(e.getMessage(), e);
    ApiResponse<Void> response = ApiResponse.fail(
        e.getCode(),
        e.getMessage()
    );
    return ResponseEntity.status(e.getHttpStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
    String errorMessage = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
    ApiResponse<Void> response = ApiResponse.fail("INPUT-400", errorMessage);
    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
    log.warn("handleAccessDeniedException", e);
    ApiResponse<Void> response = ApiResponse.fail(
        "AUTH-403",
        "접근 권한이 없습니다"
    );
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    log.error("Unhandled exception caught", e);

    // e.getMessage()가 null일 수도 있으니 기본 메시지 설정
    String message = e.getMessage() != null ? e.getMessage() : "서버 내부 오류가 발생하였습니다.";

    ApiResponse<Void> response = ApiResponse.fail(
        "SERVER-500",
        message
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}