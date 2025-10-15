package com.deliveranything.global.exception;

import com.deliveranything.global.exception.CustomException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

  public StompErrorHandler() {
    super();
  }

  @Override
  @NonNull
  public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage,
      @NonNull Throwable ex) {

    // 원인 예외 추출
    Throwable cause = ex.getCause();

    // CustomException에서 에러 코드와 메시지 추출
    if (cause instanceof CustomException customException) {
      return prepareErrorMessage(customException.getCode(), customException.getMessage());
    }

    // 그 외 일반적인 인증/메시지 예외 처리
    if (ex.getMessage() != null) {
      return prepareErrorMessage("UNAUTHORIZED", ex.getMessage());
    }

    return super.handleClientMessageProcessingError(clientMessage, ex);
  }

  private Message<byte[]> prepareErrorMessage(String code, String message) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
    accessor.setMessage(code); // 에러 코드를 message 헤더에 설정
    accessor.setLeaveMutable(true);

    // 에러 메시지를 바디에 담아 UTF-8로 인코딩
    byte[] payload = message.getBytes(StandardCharsets.UTF_8);

    return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
  }
}
