package com.autosetai.backend.auth.infrastructure.oauth;

import com.autosetai.backend.auth.domain.model.Member.Provider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KaKaoOAuthClient implements OAuthClient {
    private final RestClient restClient;

    public KaKaoOAuthClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
    }

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    @Override
    public OAuthUserProfile getUserProfile(String accessToken) {
        // 카카오 서버로 유저 accessToken 정보 검증을 위한 요청 전송
        KaKaoTokenInfoResponse response = restClient.get()
                .uri("/v1/user/access_token_info")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KaKaoTokenInfoResponse.class);

        if (response == null || response.id() == null) {
            throw new IllegalArgumentException("유효하지 않은 카카오 토큰 입니다: " + accessToken);
        }

        return new OAuthUserProfile(
                String.valueOf(response.id()),
                getProvider()
        );
    }

    private record KaKaoTokenInfoResponse(
            Long id, // kakao 회원 번호 (socialId)
            Integer expiresIn // kakao accessToken 만료 시간
    ) {}
}
