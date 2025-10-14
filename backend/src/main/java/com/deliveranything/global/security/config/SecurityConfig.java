package com.deliveranything.global.security.config;

import com.deliveranything.global.security.filter.CustomAuthenticationFilter;
import com.deliveranything.global.security.handler.CustomAccessDeniedHandler;
import com.deliveranything.global.security.handler.CustomAuthenticationEntryPoint;
import com.deliveranything.global.security.resolver.CustomOAuth2AuthorizationRequestResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomAuthenticationFilter customAuthenticationFilter;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final AuthenticationSuccessHandler customOAuth2LoginSuccessHandler;
  private final CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CSRF, FormLogin, Logout, Basic Auth 모두 비활성화
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)

        // 세션 완전 비활성화 (JWT 환경)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // OAuth2 로그인 설정 (소셜 로그인)
        .oauth2Login(oauth2Login -> oauth2Login
            .successHandler(customOAuth2LoginSuccessHandler)
            .authorizationEndpoint(authorizationEndpoint ->
                    authorizationEndpoint.authorizationRequestResolver(
                        customOAuth2AuthorizationRequestResolver)
                // 커스텀 리졸버(RedirectUrl을 State에 암호화하여 저장) 설정
            )

        )

        // CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // 권한 설정
        .authorizeHttpRequests(auth -> auth
            // 정적 리소스 및 H2 콘솔
            .requestMatchers("/favicon.ico", "/error", "/h2-console/**").permitAll()

            // Swagger
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

            // 인증/인가 관련 (로그인, 회원가입, 소셜 로그인, 토큰 재발급 등)
            .requestMatchers("/oauth2/**", "/login/oauth2/**", "/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
            .requestMatchers("/api/v1/auth/logout", "/api/v1/auth/logout/all").authenticated()

            // Actuator
            .requestMatchers("/actuator/**").permitAll()

            // OPTIONS 요청은 모두 허용 (CORS Preflight)
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // 나머지 API는 인증 필요
            .requestMatchers("/api/v1/**").authenticated()
        )

        // H2 콘솔 frame 옵션 허용
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

        // 예외 처리
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(customAuthenticationEntryPoint)
            .accessDeniedHandler(customAccessDeniedHandler)
        )

        // 커스텀 인증 필터
        .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public UrlBasedCorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 허용할 오리진 목록
    configuration.setAllowedOriginPatterns(List.of(
        "http://localhost:*",
        "https://localhost:*",
        "https://www.deliver-anything.shop",
        "https://api.deliver-anything.shop"
    ));

    // 허용할 HTTP 메서드
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

    // 허용할 헤더
    configuration.setAllowedHeaders(List.of("*"));

    // 쿠키/인증 정보 전송 허용
    configuration.setAllowCredentials(true);

    // 클라이언트 JS에서 Authorization 헤더 접근 가능
    configuration.setExposedHeaders(List.of("Authorization"));

    // Preflight 캐시 시간
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    // 모든 경로에 적용
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }


  @Bean
  public static PasswordEncoder passwordEncoder() {  // 순환 참조 해결을 위해 static 추가
    return new BCryptPasswordEncoder();
  }
}