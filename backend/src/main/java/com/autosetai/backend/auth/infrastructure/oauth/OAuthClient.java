package com.autosetai.backend.auth.infrastructure.oauth;

import com.autosetai.backend.auth.domain.model.Member.Provider;

/**
 * 전략 패턴을 활용한 인터페이스
 * 추후 KaKao 뿐만 아니라 Google, Naver 등 소셜 로그인 확장 가능성이 있기 때문에
 * DI를 통한 확장을 이용하기 위해 전략 패턴 활용
 */
public interface OAuthClient {
    // 해당 클라이언트가 담당하는 소셜 타입 반환
    Provider getProvider();

    // provider가 제공하는 토큰을 통해 우리 서비스 규격에 맞는 유저 정보를 반환
    OAuthUserProfile getUserProfile(String accessToken);
}
