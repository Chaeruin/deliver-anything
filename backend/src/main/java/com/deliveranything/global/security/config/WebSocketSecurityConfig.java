package com.deliveranything.global.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

  @Bean
  public AuthorizationManager<Message<?>> messageAuthorizationManager() {
    return new MessageMatcherDelegatingAuthorizationManager.Builder()
        // /app/**로 들어오는 메시지는 인증된 사용자만 허용
        .simpDestMatchers("/app/**").authenticated()
        // CONNECT, SUBSCRIBE 등 다른 메시지도 인증 필요
        .anyMessage().authenticated()
        .build();
  }
}