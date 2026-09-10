package com.autosetai.backend.auth.util.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // accessToken 발급
    public String createAccessToken(Long memberId) {
        return createToken(memberId, accessTokenExpiration);
    }

    // refreshToken 발급
    public String createRefreshToken(Long memberId) {
        return createToken(memberId, refreshTokenExpiration);
    }

    // 공통 토큰 발급
    private String createToken(Long memberId, long expirationTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 서명이거나 손상된 JWT 토큰입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 형식의 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰의 클레임이 비어있거나 올바르지 않습니다: {}", e.getMessage());
        }

        return false;
    }

    // 토큰에서 유저 id 추출
    public Long getMemberIdFromToken(String token) {
        Claims payload = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(payload.getSubject());
    }

}
