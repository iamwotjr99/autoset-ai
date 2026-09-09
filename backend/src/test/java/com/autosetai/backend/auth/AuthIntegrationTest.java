package com.autosetai.backend.auth;

import com.autosetai.backend.auth.controller.dto.SocialLoginRequest;
import com.autosetai.backend.auth.domain.model.Member;
import com.autosetai.backend.auth.domain.model.Member.Provider;
import com.autosetai.backend.auth.domain.repository.MemberRepository;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthClient;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthUserProfile;
import com.autosetai.backend.auth.infrastructure.redis.RefreshTokenRedisRepository;
import com.autosetai.backend.common.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @MockitoBean
    private OAuthClient oAuthClient; // 외부 카카오 API만 가짜로 대체

    @Test
    @DisplayName("신규 유저가 카카오 소셜 로그인을 요청하면, DB에 회원이 저장되고 Redis에 RefreshToken이 적재된다")
    void socialLogin_Integration_Success() throws Exception {
        // given
        String fakeKakaoToken = "valid_kakao_token";
        SocialLoginRequest request = new SocialLoginRequest(fakeKakaoToken);

        OAuthUserProfile mockProfile = new OAuthUserProfile("kakao_12345", Provider.KAKAO);
        given(oAuthClient.getUserProfile(fakeKakaoToken)).willReturn(mockProfile);

        // when (API 호출)
        mockMvc.perform(post("/api/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // then: MySQL DB에 회원이 정상 저장되었는지 검증
        List<Member> savedMembers = memberRepository.findAll();
        assertThat(savedMembers).hasSize(1);
        Member savedMember = savedMembers.getFirst();
        assertThat(savedMember.getSocialId()).isEqualTo("kakao_12345");

        // then: Redis에 생성된 회원 ID로 토큰이 세팅되었는지 검증
        Optional<String> savedRedisToken = refreshTokenRedisRepository.getRefreshTokenByMemberId(savedMember.getId());
        assertThat(savedRedisToken).isPresent();
    }
}