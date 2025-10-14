package com.deliveranything.global.config;

import com.deliveranything.domain.auth.service.AuthTokenService;
import com.deliveranything.domain.auth.service.TokenBlacklistService;
import com.deliveranything.domain.auth.service.UserAuthorityProvider;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.security.auth.SecurityUser;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final AuthTokenService authTokenService;
  private final TokenBlacklistService tokenBlacklistService;
  private final UserRepository userRepository;
  private final UserAuthorityProvider userAuthorityProvider;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic"); // 클라이언트 구독용
    registry.setApplicationDestinationPrefixes("/app"); // 메시지 수신용
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {

      @Override
      public Message<?> preSend(@Nonnull Message<?> message, @Nonnull MessageChannel channel) {

        StompCommand command = Optional.ofNullable(MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class))
            .map(StompHeaderAccessor::getCommand)
            .orElse(null);

        if (command == null) {
          // CONNECT/SUBSCRIBE/SEND가 아닌 경우 처리하지 않음
          return message;
        }

        // 인증이 필요한 명령
        if (command == StompCommand.CONNECT || command == StompCommand.SEND || command == StompCommand.SUBSCRIBE) {
          StompHeaderAccessor accessor = Optional.ofNullable(MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class))
              .orElseThrow(() -> new MessageDeliveryException("StompHeaderAccessor is null"));

          Authentication authentication = authenticate(accessor);
          accessor.setUser(authentication);
        }

        return message;
      }

      private Authentication authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
          throw new MessageDeliveryException("Unauthorized: Missing or malformed token.");
        }

        String accessToken = authorizationHeader.substring(7);

        try {
          // 블랙리스트 체크
          if (tokenBlacklistService.isBlacklisted(accessToken)) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
          }

          // 토큰 유효성 및 만료 여부 체크
          if (!authTokenService.isValidToken(accessToken) || authTokenService.isTokenExpired(accessToken)) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
          }

          // 페이로드에서 사용자 ID 추출
          Map<String, Object> payload = authTokenService.payload(accessToken);
          if (payload == null || !payload.containsKey("id")) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
          }

          Long userId = Long.parseLong(String.valueOf(payload.get("id")));

          // 사용자 정보 조회
          User user = userRepository.findByIdWithProfile(userId)
              .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

          // Authentication 객체 생성
          Collection<? extends GrantedAuthority> authorities = userAuthorityProvider.getAuthorities(user);
          UserDetails securityUser = new SecurityUser(
              user.getId(),
              user.getUsername(),
              "",
              user.getEmail(),
              user.getCurrentActiveProfile(),
              authorities
          );

          return new UsernamePasswordAuthenticationToken(securityUser, null, authorities);

        } catch (CustomException e) {
          System.err.println("WebSocket Auth Error: " + e.getMessage());
          throw new MessageDeliveryException("Unauthorized: " + e.getMessage());
        } catch (Exception e) {
          System.err.println("Unexpected WebSocket Auth Error: " + e.getMessage());
          throw new MessageDeliveryException("Unauthorized: " + e.getMessage());
        }
      }
    });
  }
}