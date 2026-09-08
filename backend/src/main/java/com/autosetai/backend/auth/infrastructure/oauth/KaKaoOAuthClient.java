package com.autosetai.backend.auth.infrastructure.oauth;

import com.autosetai.backend.auth.domain.model.Member.Provider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class KaKaoOAuthClient implements OAuthClient {
    private final RestClient restClient;

    public KaKaoOAuthClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://kapi.kakao.com")
                .build();
    }

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    @Override
    public OAuthUserProfile getUserProfile(String accessToken) {
        try {
            KaKaoTokenInfoResponse response = restClient.get()
                    .uri("/v1/user/access_token_info")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KaKaoTokenInfoResponse.class);

            if (response == null || response.id() == null) {
                throw new IllegalArgumentException("유효하지 않은 카카오 토큰 입니다: " + accessToken);
            }

            return new OAuthUserProfile(String.valueOf(response.id()), getProvider());

        } catch (HttpClientErrorException e) {
            // 4xx 에러(만료, 잘못된 토큰 등) 발생 시 일관된 예외로 변환
            throw new IllegalArgumentException("카카오 서버 인증에 실패했습니다.: " + accessToken);
        }
    }

    private record KaKaoTokenInfoResponse(
            Long id, // kakao 회원 번호 (socialId)
            Integer expiresIn // kakao accessToken 만료 시간
    ) {}
}
