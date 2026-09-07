package com.autosetai.backend.auth.service;

import com.autosetai.backend.auth.domain.model.Member;
import com.autosetai.backend.auth.domain.repository.MemberRepository;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthClient;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthUserProfile;
import com.autosetai.backend.auth.infrastructure.redis.RefreshTokenRedisRepository;
import com.autosetai.backend.auth.service.dto.AuthTokenResponse;
import com.autosetai.backend.auth.util.RandomNicknameGenerator;
import com.autosetai.backend.auth.util.jwt.JwtProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final OAuthClient oAuthClient;
    private final RandomNicknameGenerator nicknameGenerator;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisRepository refreshTokenRepository;


    // 추후 소셜 로그인 제공자가 카카오뿐만 아니라 추가가 된다면 Provider를 변수로 받아야함
    @Transactional
    public AuthTokenResponse socialLogin(String socialAccessToken) {

        // 1. socialAccessToken으로 social 인증
        OAuthUserProfile userProfile = oAuthClient.getUserProfile(socialAccessToken);

        // 2. social_id를 통해 우리 서비스의 유저인지 확인
        // 서비스의 유저가 아니라면 회원가입(Member 생성 및 저장)
        Member member = memberRepository.findBySocialId(userProfile.socialId())
                .orElseGet(() -> {
                    String nickname = nicknameGenerator.generate();

                    Member newMember = Member.create(userProfile.socialId(), userProfile.provider(),
                            nickname);

                    return memberRepository.save(newMember);
                });

        // 3. 로그인 성공: accessToken 발급, refreshToken 저장
        return issueTokens(member.getId());
    }

    @Transactional
    public AuthTokenResponse reissueTokens(String refreshToken) {
        // 요청된 refreshToken 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token 입니다.");
        }
        Long memberId = jwtProvider.getMemberIdFromToken(refreshToken);
        String savedToken = refreshTokenRepository.getRefreshTokenByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("로그아웃되었거나 만료된 토큰입니다."));

        // 요청된 토큰과 해당 memberId로 등록된 토큰이 동일한지 비교
        if (!savedToken.equals(refreshToken)) {
            refreshTokenRepository.deleteRefreshTokenByMemberId(memberId);
            throw new IllegalArgumentException("토큰 위조 가능성이 감지되었습니다.");
        }

        // 토큰 재발급
        return issueTokens(memberId);
    }


    private AuthTokenResponse issueTokens(Long memberId) {
        String accessToken = jwtProvider.createAccessToken(memberId);
        String refreshToken = jwtProvider.createRefreshToken(memberId);

        refreshTokenRepository.save(memberId, refreshToken);

        return AuthTokenResponse.of(accessToken, refreshToken);
    }

}
