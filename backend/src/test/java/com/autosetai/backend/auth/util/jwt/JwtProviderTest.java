package com.autosetai.backend.auth.util.jwt;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;
    private final String SECRET = "AutoSetAiSuperSecretKeyForJwtSignatureValidations2026!";
    private final long ACCESS_EXP = 3600000L; // 1시간
    private final long REFRESH_EXP = 1209600000L; // 14일

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, ACCESS_EXP, REFRESH_EXP);
    }

    @Test
    @DisplayName("Access Token과 Refresh Token이 정상적으로 발급되고 검증된다")
    void createAndValidateTokens_Success() {
        // given
        Long memberId = 1L;

        // when
        String accessToken = jwtProvider.createAccessToken(memberId);
        String refreshToken = jwtProvider.createRefreshToken(memberId);

        // then
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(jwtProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtProvider.validateToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("정상적인 토큰에서 유저 ID(PK)를 정확히 추출한다")
    void getMemberIdFromToken_Success() {
        // given
        Long memberId = 99L;
        String token = jwtProvider.createAccessToken(memberId);

        // when
        Long extractedId = jwtProvider.getMemberIdFromToken(token);

        // then
        assertThat(extractedId).isEqualTo(memberId);
    }

    @Test
    @DisplayName("조작된 토큰은 검증에 실패한다 (SecurityException / MalformedJwtException 분기)")
    void validateToken_Fail_InvalidSignature() {
        // given: 정상 토큰 발급 후 문자열 변조
        String validToken = jwtProvider.createAccessToken(1L);
        String invalidToken = validToken + "manipulated";

        // when
        boolean isValid = jwtProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다 (ExpiredJwtException 분기)")
    void validateToken_Fail_ExpiredToken() {
        // given: 만료 시간을 과거(-1000ms)로 설정하여 즉시 만료되는 프로바이더 생성
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1000L, -1000L);
        String expiredToken = expiredProvider.createAccessToken(1L);

        // when
        boolean isValid = jwtProvider.validateToken(expiredToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("서명이 없는 토큰은 검증에 실패한다 (UnsupportedJwtException 분기)")
    void validateToken_Fail_UnsupportedToken() {
        // given: signWith()를 생략하여 서명되지 않은 껍데기 토큰 생성
        String unsignedToken = Jwts.builder()
                .subject("1")
                .compact();

        // when
        boolean isValid = jwtProvider.validateToken(unsignedToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 토큰은 검증에 실패한다 (IllegalArgumentException 분기)")
    void validateToken_Fail_EmptyToken() {
        // given
        String emptyToken = "";

        // when
        boolean isValid = jwtProvider.validateToken(emptyToken);

        // then
        assertThat(isValid).isFalse();
    }
}
