package com.autosetai.backend.auth.service;

import com.autosetai.backend.auth.domain.model.Member;
import com.autosetai.backend.auth.domain.repository.MemberRepository;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthClient;
import com.autosetai.backend.auth.infrastructure.oauth.OAuthUserProfile;
import com.autosetai.backend.auth.util.RandomNicknameGenerator;
import com.autosetai.backend.auth.util.jwt.JwtProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final OAuthClient oAuthClient;
    private final RandomNicknameGenerator nicknameGenerator;
    private final JwtProvider jwtProvider;

    public String socialLogin(String socialAccessToken) {

        // 1. socialAccessToken으로 social 인증
        OAuthUserProfile userProfile = oAuthClient.getUserProfile(socialAccessToken);

        // 2. social_id를 통해 우리 서비스의 유저인지 확인
        // 우리 서비스의 유저가 아니라면 회원가입(Member 생성 및 저장)
        Member member = memberRepository.findBySocialId(userProfile.socialId())
                .orElseGet(() -> {
                    String nickname = nicknameGenerator.generate();

                    Member newMember = Member.create(userProfile.socialId(), userProfile.provider(),
                            nickname);

                    return memberRepository.save(newMember);
                });

        /**
         * 토큰 생성 로직
         */
        // 3. 로그인 성공: accessToken 발급, refreshToken 저장
        String accessToken = jwtProvider.createAccessToken(member.getId());
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        return accessToken;
    }

}
