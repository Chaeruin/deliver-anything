package com.deliveranything.global.security.handler;

import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode; // New import
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Slf4j
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    public StompErrorHandler() {
        super();
    }

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable actualException = resolveNotWrappedException(ex);

        if (actualException instanceof CustomException customException) {
            log.error("STOMP Custom Error: {} - {}", customException.getCode(), customException.getMessage());
            return handleCustomException(clientMessage, customException);
        } else if (actualException instanceof org.springframework.security.access.AccessDeniedException) {
            log.error("STOMP Access Denied: {}", actualException.getMessage());
            return handleAccessDeniedException(clientMessage, (org.springframework.security.access.AccessDeniedException) actualException);
        } else {
            log.error("STOMP Generic Error: {}", actualException.getMessage(), actualException);
            return handleOtherException(clientMessage, actualException);
        }
    }

    private Throwable resolveNotWrappedException(Throwable ex) {
        if (ex instanceof org.springframework.messaging.MessagingException && ex.getCause() != null) {
            return ex.getCause();
        }
        return ex;
    }

    private Message<byte[]> handleCustomException(Message<byte[]> clientMessage, CustomException ex) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(ex.getMessage());
        accessor.setLeaveMutable(true); // Allow modification of headers

        // Add custom headers for frontend to parse
        accessor.addNativeHeader("error-code", ex.getCode());
        accessor.addNativeHeader("error-message", ex.getMessage());

        // Optionally, include the original destination if available
        Optional.of(StompHeaderAccessor.wrap(clientMessage)).map(StompHeaderAccessor::getDestination)
                .ifPresent(destination -> accessor.addNativeHeader("destination", destination));

        return MessageBuilder.createMessage(
                ex.getMessage().getBytes(StandardCharsets.UTF_8),
                accessor.getMessageHeaders()
        );
    }

    private Message<byte[]> handleAccessDeniedException(Message<byte[]> clientMessage, AccessDeniedException ex) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage("Access Denied: " + ex.getMessage());
        accessor.setLeaveMutable(true);

        accessor.addNativeHeader("error-code", ErrorCode.ACCESS_DENIED.getCode());
        accessor.addNativeHeader("error-message", ErrorCode.ACCESS_DENIED.getMessage());

        Optional.of(StompHeaderAccessor.wrap(clientMessage)).map(StompHeaderAccessor::getDestination)
                .ifPresent(destination -> accessor.addNativeHeader("destination", destination));

        return MessageBuilder.createMessage(
                ("Access Denied: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8),
                accessor.getMessageHeaders()
        );
    }

    private Message<byte[]> handleOtherException(Message<byte[]> clientMessage, Throwable ex) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage("Internal Server Error");
        accessor.setLeaveMutable(true);

        accessor.addNativeHeader("error-code", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        accessor.addNativeHeader("error-message", ErrorCode.INTERNAL_SERVER_ERROR.getMessage());

        Optional.of(StompHeaderAccessor.wrap(clientMessage)).map(StompHeaderAccessor::getDestination)
                .ifPresent(destination -> accessor.addNativeHeader("destination", destination));

        return MessageBuilder.createMessage(
                ("Internal Server Error: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8),
                accessor.getMessageHeaders()
        );
    }
}
