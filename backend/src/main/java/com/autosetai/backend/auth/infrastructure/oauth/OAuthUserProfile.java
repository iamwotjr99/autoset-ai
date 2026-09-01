package com.autosetai.backend.auth.infrastructure.oauth;

import com.autosetai.backend.auth.domain.model.Member.Provider;

/**
 * 소셜 로그인 공통 DTO
 * @param socialId - 소셜 Id
 * @param provider - 소셜 로그인 제공자 (KaKao, Goggle ...)
 */
public record OAuthUserProfile(
        String socialId,
        Provider provider
) {

}
