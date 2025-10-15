package com.deliveranything.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
        title = "뭐든배달 API 명세서",
        version = "v1",
        description = "뭐든배달 API 명세서입니다"
    )
)
@Configuration
@Slf4j
public class SwaggerConfig {

  public static final String JWT_SECURITY_SCHEME = "JWT Token";

  @Bean
  public OpenAPI openAPI() {
    SecurityScheme apiKey = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)      // HTTP 인증 방식
        .in(SecurityScheme.In.HEADER)        // Header에 포함
        .name("Authorization")               // Header 이름
        .scheme("bearer")                    // Bearer 인증
        .bearerFormat("JWT");                // 토큰 형식

    // Swagger에서 인증이 필요함을 명시
    SecurityRequirement securityRequirement = new SecurityRequirement()
        .addList("Bearer Token");

    // OpenAPI 객체 구성
    return new OpenAPI()
        .addServersItem(new Server().url("https://api.deliver-anything.shop"))
        .components(new Components().addSecuritySchemes("Bearer Token", apiKey))
        .addSecurityItem(securityRequirement);
  }
}
