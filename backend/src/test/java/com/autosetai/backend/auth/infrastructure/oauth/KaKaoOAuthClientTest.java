package com.autosetai.backend.auth.infrastructure.oauth;

import com.autosetai.backend.auth.domain.model.Member.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KaKaoOAuthClientTest {

    private KaKaoOAuthClient kaKaoOAuthClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // given: Mock 서버가 연결된 RestClient.Builder 생성
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        // 의존성 주입을 통해 클라이언트 객체 생성
        kaKaoOAuthClient = new KaKaoOAuthClient(builder);
    }

    @Test
    @DisplayName("Provider 타입이 KAKAO로 정확히 반환된다")
    void getProvider_Success() {
        // when
        Provider provider = kaKaoOAuthClient.getProvider();

        // then
        assertThat(provider).isEqualTo(Provider.KAKAO);
    }

    @Test
    @DisplayName("정상적인 카카오 AccessToken이 주어지면, 사용자 프로필을 반환한다")
    void getUserProfile_Success() {
        // given
        String validToken = "valid_kakao_token";
        String mockResponseJson = """
                {
                    "id": 1234,
                    "expires_in": 3600
                }
                """;

        // 카카오 서버가 받을 요청과 내려줄 가짜 응답을 세팅
        mockServer.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        // when
        OAuthUserProfile profile = kaKaoOAuthClient.getUserProfile(validToken);

        // then
        assertThat(profile.socialId()).isEqualTo("kakao_1234");
        assertThat(profile.provider()).isEqualTo(Provider.KAKAO);
        mockServer.verify(); // 예상한 요청이 실제로 실행되었는지 검증
    }

    @Test
    @DisplayName("카카오 응답 JSON에 id 필드가 null이면 예외가 발생한다")
    void getUserProfile_Fail_IdIsNull() {
        // given
        String token = "token_without_id";
        String mockResponseJson = """
                {
                    "id": null,
                    "expires_in": 3600
                }
                """;

        mockServer.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> kaKaoOAuthClient.getUserProfile(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 카카오 토큰");
    }

    @Test
    @DisplayName("만료되거나 잘못된 토큰으로 카카오 서버가 401 예외를 반환하면 예외가 발생한다")
    void getUserProfile_Fail_Unauthorized() {
        // given
        String invalidToken = "expired_token";

        // 카카오 서버가 401 Unauthorized 에러를 내려주는 상황 모방
        mockServer.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(() -> kaKaoOAuthClient.getUserProfile(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("카카오 서버 인증에 실패했습니다");
    }
}