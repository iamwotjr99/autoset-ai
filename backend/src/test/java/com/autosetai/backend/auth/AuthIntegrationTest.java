package com.autosetai.backend.auth;

import com.autosetai.backend.auth.controller.dto.ReissueTokensRequest;
import com.autosetai.backend.auth.controller.dto.SocialLoginRequest;
import com.autosetai.backend.auth.domain.model.Member;
import com.autosetai.backend.auth.domain.model.Member.Provider;
import com.autosetai.backend.auth.domain.repository.MemberRepository;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthClient;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthUserProfile;
import com.autosetai.backend.auth.infrastructure.redis.RefreshTokenRedisRepository;
import com.autosetai.backend.auth.util.jwt.JwtProvider;
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

    @Autowired
    private JwtProvider jwtProvider;

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

    @Test
    @DisplayName("유효한 리프레시 토큰으로 요청하면, 새로운 토큰 쌍이 발급되고 Redis가 갱신된다")
    void reissueTokens_Integration_Success() throws Exception {
        // given 1: DB에 가입된 회원을 강제로 하나 밀어넣음
        Member member = Member.builder()
                .socialId("kakao_1234")
                .provider(Member.Provider.KAKAO)
                .nickname("득근득근_1234")
                .build();
        memberRepository.save(member);

        // given 2: JwtProvider로 해당 유저의 리프레시 토큰을 생성
        String validRefreshToken = jwtProvider.createRefreshToken(member.getId());

        // given 3: 생성된 토큰을 Redis에 적재
        refreshTokenRedisRepository.save(member.getId(), validRefreshToken);

        // API 요청 DTO 생성
        ReissueTokensRequest request = new ReissueTokensRequest(validRefreshToken);

        // when & then: 컨트롤러로 HTTP POST 요청 전송
        mockMvc.perform(post("/api/auth/reissue-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                // 1. 응답에 액세스/리프레시 토큰이 모두 존재하는지 확인
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                // 2. 발급된 리프레시 토큰이 기존 토큰과 '다른' 새로운 토큰인지 확인 (RTR 방식)
                .andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.not(validRefreshToken)));

        // then (DB 검증): Redis에 새로운 토큰으로 잘 덮어씌워졌는지 최종 확인
        Optional<String> tokenInRedis = refreshTokenRedisRepository.getRefreshTokenByMemberId(member.getId());
        assertThat(tokenInRedis).isPresent();
        assertThat(tokenInRedis.get()).isNotEqualTo(validRefreshToken);
    }
}