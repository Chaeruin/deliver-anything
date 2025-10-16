package com.deliveranything.global.config;

import com.deliveranything.domain.auth.auth.service.AccessTokenService;
import com.deliveranything.domain.auth.auth.service.TokenBlacklistService;
import com.deliveranything.domain.auth.auth.service.UserAuthorityProvider;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.security.auth.SecurityUser;
import com.deliveranything.global.security.handler.StompErrorHandler;
import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
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

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final AccessTokenService accessTokenService;
  private final TokenBlacklistService tokenBlacklistService;
  private final UserRepository userRepository;
  private final UserAuthorityProvider userAuthorityProvider;
  private final StompErrorHandler stompErrorHandler;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.setErrorHandler(stompErrorHandler);
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("http://localhost:*",
            "https://localhost:*",
            "https://cdpn.io",
            "https://www.deliver-anything.shop",
            "https://api.deliver-anything.shop"
        )
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

        StompHeaderAccessor accessor = Optional.ofNullable(
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class))
            .orElseThrow(() -> new MessageDeliveryException("StompHeaderAccessor is null"));

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
          log.debug("STOMP CONNECT command received. SessionId: {}", accessor.getSessionId());
          String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
          log.debug("Authorization Header: {}", authorizationHeader);

          // CONNECT 명령에 대해서만 전체 토큰 인증을 수행합니다.
          Authentication authentication = authenticate(accessor);
          accessor.setUser(authentication);
          log.debug("User authenticated for CONNECT. Principal: {}, Authenticated: {}", authentication.getPrincipal(), authentication.isAuthenticated());
        } else if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
          log.debug("STOMP {} command received. SessionId: {}, Destination: {}", command, accessor.getSessionId(), accessor.getDestination());
          // SEND/SUBSCRIBE의 경우, 세션에서 기존 인증 정보를 가져옵니다.
          Authentication authentication = (Authentication) accessor.getUser();
          log.debug("Retrieved Authentication for {}. Principal: {}, Authenticated: {}", command, authentication != null ? authentication.getPrincipal() : "N/A", authentication != null ? authentication.isAuthenticated() : "N/A");

          if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized: No active authenticated session for {} command. SessionId: {}", command, accessor.getSessionId());
            throw new MessageDeliveryException("Unauthorized: No active authenticated session.");
          }
          // 이미 인증된 세션이므로 추가적인 authenticate 호출은 필요 없습니다.
        }
        // 다른 명령 (예: DISCONNECT)은 특별한 처리 없이 메시지를 반환합니다.

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
          if (!accessTokenService.isValidToken(accessToken) || accessTokenService.isTokenExpired(
              accessToken)) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
          }

          // 페이로드에서 사용자 ID 추출
          Map<String, Object> payload = accessTokenService.payload(accessToken);
          if (payload == null || !payload.containsKey("id")) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
          }

          Long userId = Long.parseLong(String.valueOf(payload.get("id")));

          // 사용자 정보 조회
          User user = userRepository.findByIdWithProfile(userId)
              .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

          // Authentication 객체 생성
          Collection<? extends GrantedAuthority> authorities = userAuthorityProvider.getAuthorities(
              user);
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
          log.error("WebSocket Auth Error: {}", e.getMessage(), e);
          throw e; // Throw CustomException directly
        } catch (Exception e) {
          log.error("Unexpected WebSocket Auth Error: {}", e.getMessage(), e);
          // Wrap generic exceptions in CustomException for consistent error handling
          throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
      }
    });
  }

  /**
   * Disables CSRF for STOMP messages when using @EnableWebSocketSecurity.
   * This bean name is specifically recognized by Spring Security to override the default CSRF ChannelInterceptor.
   */
  @Bean
  public ChannelInterceptor csrfChannelInterceptor() {
      // Returning a no-op ChannelInterceptor effectively disables the CSRF check
      return new ChannelInterceptor() {};
  }
}