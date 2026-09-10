package com.autosetai.backend.auth.controller;

import com.autosetai.backend.auth.controller.dto.ReissueTokensRequest;
import com.autosetai.backend.auth.controller.dto.SocialLoginRequest;
import com.autosetai.backend.auth.service.AuthService;
import com.autosetai.backend.auth.service.dto.AuthTokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class) // 컨트롤러(웹 계층)만 가볍게 로드
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가짜 HTTP 요청을 보내는 객체


    private ObjectMapper objectMapper = new ObjectMapper(); // 객체를 JSON 문자열로 변환해주는 도구

    @MockitoBean
    private AuthService authService; // 컨트롤러가 의존하는 서비스는 가짜(Mock)로 주입

    // =========================================================================
    // 1. socialLogin() API 테스트
    // =========================================================================

    @Test
    @DisplayName("소셜 로그인 성공 시 200 OK와 토큰 쌍을 반환한다")
    void socialLogin_Success() throws Exception {
        // given
        SocialLoginRequest request = new SocialLoginRequest("valid_kakao_token");
        AuthTokenResponse mockResponse = AuthTokenResponse.of("access_token", "refresh_token");

        given(authService.socialLogin(anyString())).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // DTO를 JSON으로 변환하여 바디에 담음
                .andDo(print()) // 실행 결과를 콘솔에 자세히 출력 (디버깅용)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access_token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh_token"));
    }

    @Test
    @DisplayName("소셜 로그인 요청 시 socialAccessToken이 없으면 400 Bad Request가 발생한다 (@Valid 검증)")
    void socialLogin_Fail_NullToken() throws Exception {
        // given
        SocialLoginRequest request = new SocialLoginRequest(null); // 토큰 누락!

        // when & then
        mockMvc.perform(post("/api/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 스프링의 @Valid가 400 에러를 터뜨려야 정상
    }

    // =========================================================================
    // 2. reissueTokens() API 테스트
    // =========================================================================

    @Test
    @DisplayName("토큰 재발급 성공 시 200 OK와 새로운 토큰 쌍을 반환한다")
    void reissueTokens_Success() throws Exception {
        // given
        ReissueTokensRequest request = new ReissueTokensRequest("valid_refresh_token");
        AuthTokenResponse mockResponse = AuthTokenResponse.of("new_access", "new_refresh");

        given(authService.reissueTokens(anyString())).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/auth/reissue-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new_access"))
                .andExpect(jsonPath("$.refreshToken").value("new_refresh"));
    }

    @Test
    @DisplayName("토큰 재발급 요청 시 refreshToken이 없으면 400 Bad Request가 발생한다 (@Valid 검증)")
    void reissueTokens_Fail_NullToken() throws Exception {
        // given
        ReissueTokensRequest request = new ReissueTokensRequest(null); // 리프레시 토큰 누락!

        // when & then
        mockMvc.perform(post("/api/auth/reissue-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}