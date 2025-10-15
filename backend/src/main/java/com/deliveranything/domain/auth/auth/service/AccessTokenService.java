package com.deliveranything.domain.auth.auth.service;

import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccessTokenService {

  @Value("${custom.jwt.secretKey}")
  private String jwtSecretKey;

  @Value("${custom.accessToken.expirationSeconds}")
  private int accessTokenExpirationSeconds;

  /**
   * 사용자 정보를 기반으로 JWT Access Token 생성 (전역 Profile ID 사용)
   */
  public String genAccessToken(User user) {
    long id = user.getId();
    String username = user.getUsername();

    Profile activeProfile = user.getCurrentActiveProfile();

    // 멀티 프로필 정보 (이제 전역 고유 Profile ID)
    ProfileType currentActiveProfileType = null;
    Long currentActiveProfileId = null;

    if (activeProfile != null) {
      currentActiveProfileType = activeProfile.getType(); // Profile.getType() 호출
      currentActiveProfileId = activeProfile.getId(); // Profile.getId() 호출
    }

    ClaimsBuilder claimsBuilder = Jwts.claims();

    // 필수 정보 + UI용 name
    claimsBuilder.add("id", id);
    claimsBuilder.add("name", username);
    claimsBuilder.add("currentActiveProfile",
        currentActiveProfileType != null ? currentActiveProfileType.name() : null);
    claimsBuilder.add("currentActiveProfileId", currentActiveProfileId); // null 가능

    Claims claims = claimsBuilder.build();

    Date issuedAt = new Date();
    Date expiration = new Date(issuedAt.getTime() + 1000L * accessTokenExpirationSeconds);

    Key secretKey = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());

    return Jwts.builder()
        .claims(claims)
        .issuedAt(issuedAt)
        .expiration(expiration)
        .signWith(secretKey)
        .compact();
  }

  /**
   * JWT Access Token에서 페이로드 파싱 필수 정보 + name 추출 (전역 Profile ID 포함)
   */
  public Map<String, Object> payload(String accessToken) {
    SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());

    try {
      // ✅ parseClaimsJws() 사용 (서명 검증 포함)
      Claims claims = Jwts
          .parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(accessToken)  // ✅ 변경: parse() → parseSignedClaims()
          .getPayload();

      // 필수 정보 + UI용 name 추출
      Long id = claims.get("id", Long.class);
      String username = claims.get("name", String.class);

      // ✅ 멀티 프로필 정보 추출 - String으로 가져오기
      String currentActiveProfileStr = claims.get("currentActiveProfile", String.class);
      Long currentActiveProfileId = claims.get("currentActiveProfileId", Long.class);

      // ✅ HashMap 사용 (null 값 허용)
      Map<String, Object> result = new HashMap<>();
      result.put("id", id);
      result.put("name", username != null ? username : "");
      result.put("currentActiveProfile", currentActiveProfileStr);  // ✅ String 그대로
      result.put("currentActiveProfileId", currentActiveProfileId);  // ✅ null 가능

      return result;

    } catch (Exception e) {
      log.warn("JWT 토큰 파싱 실패: {}", e.getMessage(), e);  // 스택 트레이스 추가
      return null;
    }
  }

  // JWT 토큰 유효성 검증
  public boolean isValidToken(String accessToken) {
    SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());

    try {
      Jwts
          .parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(accessToken);  // 변경: parse() → parseSignedClaims()
      return true;
    } catch (Exception e) {
      log.debug("JWT 토큰 검증 실패: {}", e.getMessage());
      return false;
    }
  }

  // JWT 토큰에서 만료 시간 추출
  public Date getExpirationDate(String accessToken) {
    SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());

    try {
      Claims claims = Jwts
          .parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(accessToken)  // 변경: parse() → parseSignedClaims()
          .getPayload();

      return claims.getExpiration();
    } catch (Exception e) {
      log.warn("JWT 만료 시간 추출 실패: {}", e.getMessage());
      return null;
    }
  }

  // 토큰이 만료되었는지 확인
  public boolean isTokenExpired(String accessToken) {
    Date expiration = getExpirationDate(accessToken);
    return expiration != null && expiration.before(new Date());
  }

  // JWT 토큰에서 사용자 ID 추출 (빠른 조회)
  public Long getUserId(String accessToken) {
    Map<String, Object> payload = payload(accessToken);
    return payload != null ? (Long) payload.get("id") : null;
  }

  // JWT 토큰에서 현재 활성 프로필 타입 추출 (권한 체크용)
  public ProfileType getCurrentActiveProfile(String accessToken) {
    Map<String, Object> payload = payload(accessToken);
    return payload != null ? (ProfileType) payload.get("currentActiveProfile") : null;
  }

  // JWT 토큰에서 현재 활성 프로필 ID 추출 (전역 고유 ID)
  public Long getCurrentActiveProfileId(String accessToken) {
    Map<String, Object> payload = payload(accessToken);
    return payload != null ? (Long) payload.get("currentActiveProfileId") : null;
  }

  /**
   * 토큰의 만료 시간 반환 (밀리초)
   */
  public long getExpirationTime(String token) {
    SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());

    try {
      Claims claims = Jwts
          .parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();

      return claims.getExpiration().getTime();
    } catch (Exception e) {
      log.error("토큰 만료 시간 조회 실패: {}", e.getMessage());
      return 0;
    }
  }
}