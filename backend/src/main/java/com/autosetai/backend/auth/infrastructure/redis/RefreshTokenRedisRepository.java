package com.autosetai.backend.auth.infrastructure.redis;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRedisRepository {
    private static final String KEY_PREFIX = "RT:";

    private final StringRedisTemplate stringTemplate;
    private final long refreshTokenExpiration;

    public RefreshTokenRedisRepository(
            StringRedisTemplate stringRedisTemplate,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration
    ) {
        this.stringTemplate = stringRedisTemplate;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // refreshToken 저장
    public void save(Long memberId, String refreshToken) {
        String key = KEY_PREFIX + memberId;
        stringTemplate.opsForValue()
                .set(key, refreshToken, Expiration.from(refreshTokenExpiration, TimeUnit.MILLISECONDS));
    }

    // memberId를 통해 refreshToken 조회
    public Optional<String> getRefreshTokenByMemberId(Long memberId) {
        return Optional.ofNullable(stringTemplate.opsForValue().get(KEY_PREFIX + memberId));
    }

    // memberId를 통해 refreshToken 삭제
    public void deleteRefreshTokenByMemberId(Long memberId) {
        stringTemplate.delete(KEY_PREFIX + memberId);
    }
}
