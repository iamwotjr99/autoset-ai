package com.autosetai.backend.auth.service;

import com.autosetai.backend.auth.domain.model.Member;
import com.autosetai.backend.auth.domain.repository.MemberRepository;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthClient;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthUserProfile;
import com.autosetai.backend.auth.infrastructure.redis.RefreshTokenRedisRepository;
import com.autosetai.backend.auth.service.dto.AuthTokenResponse;
import com.autosetai.backend.auth.util.RandomNicknameGenerator;
import com.autosetai.backend.auth.util.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private MemberRepository memberRepository;
    @Mock private OAuthClient oAuthClient;
    @Mock private RandomNicknameGenerator nicknameGenerator;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRedisRepository refreshTokenRepository;

    // =========================================================================
    // 1. socialLogin() 테스트
    // =========================================================================

    @Test
    @DisplayName("신규 유저 로그인 시, 회원가입이 진행되고 토큰이 발급된다")
    void socialLogin_NewUser_Success() {
        // given
        String socialToken = "kakao_token";
        OAuthUserProfile profile = new OAuthUserProfile("social_123", Member.Provider.KAKAO);
        Member newMember = Member.create(profile.socialId(), profile.provider(), "득근하는_헬린이");
        ReflectionTestUtils.setField(newMember, "id", 1L); // PK 강제 주입

        given(oAuthClient.getUserProfile(socialToken)).willReturn(profile);
        given(memberRepository.findBySocialId(profile.socialId())).willReturn(Optional.empty()); // DB에 없음
        given(nicknameGenerator.generate()).willReturn("득근하는_헬린이");
        given(memberRepository.save(any(Member.class))).willReturn(newMember);

        given(jwtProvider.createAccessToken(1L)).willReturn("access_token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh_token");

        // when
        AuthTokenResponse response = authService.socialLogin(socialToken);

        // then
        assertThat(response.accessToken()).isEqualTo("access_token");
        assertThat(response.refreshToken()).isEqualTo("refresh_token");
        verify(memberRepository).save(any(Member.class)); // ⭐️ save 호출 여부 검증
        verify(refreshTokenRepository).save(1L, "refresh_token"); // Redis 저장 검증
    }

    @Test
    @DisplayName("기존 유저 로그인 시, 회원가입(save) 없이 즉시 토큰이 발급된다")
    void socialLogin_ExistingUser_Success() {
        // given
        String socialToken = "kakao_token";
        OAuthUserProfile profile = new OAuthUserProfile("social_123", Member.Provider.KAKAO);
        Member existingMember = Member.create(profile.socialId(), profile.provider(), "기존_유저");
        ReflectionTestUtils.setField(existingMember, "id", 1L);

        given(oAuthClient.getUserProfile(socialToken)).willReturn(profile);
        given(memberRepository.findBySocialId(profile.socialId())).willReturn(Optional.of(existingMember)); // DB에 존재함

        given(jwtProvider.createAccessToken(1L)).willReturn("access_token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh_token");

        // when
        authService.socialLogin(socialToken);

        // then
        verify(memberRepository, never()).save(any(Member.class)); // ⭐️ save가 절대 호출되지 않음을 검증 (분기 완벽 커버)
    }

    // =========================================================================
    // 2. reissueTokens() 테스트
    // =========================================================================

    @Test
    @DisplayName("정상적인 Refresh Token 요청 시, 새로운 토큰 쌍을 발급한다")
    void reissueTokens_Success() {
        // given
        String validToken = "valid_refresh_token";
        Long memberId = 1L;

        given(jwtProvider.validateToken(validToken)).willReturn(true);
        given(jwtProvider.getMemberIdFromToken(validToken)).willReturn(memberId);
        given(refreshTokenRepository.getRefreshTokenByMemberId(memberId)).willReturn(Optional.of(validToken)); // Redis에 동일한 토큰 존재

        given(jwtProvider.createAccessToken(memberId)).willReturn("new_access");
        given(jwtProvider.createRefreshToken(memberId)).willReturn("new_refresh");

        // when
        AuthTokenResponse response = authService.reissueTokens(validToken);

        // then
        assertThat(response.accessToken()).isEqualTo("new_access");
        assertThat(response.refreshToken()).isEqualTo("new_refresh");
        verify(refreshTokenRepository).save(memberId, "new_refresh"); // RTR 갱신 확인
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 재발급 요청 시 예외가 발생한다")
    void reissueTokens_Fail_InvalidToken() {
        // given
        String invalidToken = "invalid_token";
        given(jwtProvider.validateToken(invalidToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissueTokens(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않거나 만료된 Refresh Token");
    }

    @Test
    @DisplayName("Redis에 저장된 토큰이 없으면(로그아웃 상태) 예외가 발생한다")
    void reissueTokens_Fail_NotFoundInRedis() {
        // given
        String validToken = "valid_refresh_token";
        Long memberId = 1L;

        given(jwtProvider.validateToken(validToken)).willReturn(true);
        given(jwtProvider.getMemberIdFromToken(validToken)).willReturn(memberId);
        given(refreshTokenRepository.getRefreshTokenByMemberId(memberId)).willReturn(Optional.empty()); // Redis 텅 빔

        // when & then
        assertThatThrownBy(() -> authService.reissueTokens(validToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("로그아웃되었거나 만료된 토큰");
    }

    @Test
    @DisplayName("Redis에 저장된 토큰과 요청한 토큰이 다르면 강제 삭제 후 예외가 발생한다 (RTR 보안)")
    void reissueTokens_Fail_TokenMismatch() {
        // given
        String requestedToken = "hacked_token";
        String savedToken = "real_token";
        Long memberId = 1L;

        given(jwtProvider.validateToken(requestedToken)).willReturn(true);
        given(jwtProvider.getMemberIdFromToken(requestedToken)).willReturn(memberId);
        given(refreshTokenRepository.getRefreshTokenByMemberId(memberId)).willReturn(Optional.of(savedToken)); // 다른 토큰 반환

        // when & then
        assertThatThrownBy(() -> authService.reissueTokens(requestedToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("토큰 위조 가능성");

        verify(refreshTokenRepository).deleteRefreshTokenByMemberId(memberId); // ⭐️ 즉시 삭제되어 보안을 유지하는지 검증
    }
}