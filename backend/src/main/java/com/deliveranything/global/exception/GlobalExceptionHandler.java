package com.deliveranything.global.exception;

import com.deliveranything.global.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final ObjectMapper objectMapper;

  @ExceptionHandler(CustomException.class)
  public Object handleCustomException(CustomException e, HttpServletRequest request) {
    log.info(e.getMessage(), e);

    if (isSseRequest(request)) {
      return createSseErrorResponse(e.getCode(), e.getMessage());
    }

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
  public Object handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
    log.warn("handleAccessDeniedException", e);

    if (isSseRequest(request)) {
      return createSseErrorResponse("AUTH-403", "접근 권한이 없습니다");
    }

    ApiResponse<Void> response = ApiResponse.fail(
        "AUTH-403",
        "접근 권한이 없습니다"
    );
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  @ExceptionHandler(Exception.class)
  public Object handleException(Exception e, HttpServletRequest request) {
    log.error("Unhandled exception caught", e);

    String message = e.getMessage() != null ? e.getMessage() : "서버 내부 오류가 발생하였습니다.";

    if (isSseRequest(request)) {
      return createSseErrorResponse("SERVER-500", message);
    }

    ApiResponse<Void> response = ApiResponse.fail(
        "SERVER-500",
        message
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  private boolean isSseRequest(HttpServletRequest request) {
    String acceptHeader = request.getHeader("Accept");
    return acceptHeader != null && acceptHeader.contains("text/event-stream");
  }

  private SseEmitter createSseErrorResponse(String code, String message) {
    SseEmitter emitter = new SseEmitter();
    try {
      String jsonError = objectMapper.writeValueAsString(Map.of(
          "code", code,
          "message", message
      ));
      emitter.send(SseEmitter.event().name("error").data(jsonError));
      emitter.complete();
    } catch (IOException ex) {
      log.error("Error while sending SSE error response", ex);
      emitter.completeWithError(ex);
    }
    return emitter;
  }
}