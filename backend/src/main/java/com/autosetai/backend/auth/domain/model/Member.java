package com.autosetai.backend.auth.domain.model;

import com.autosetai.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "social_id", length = 100)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Builder
    private Member(String socialId, Provider provider, String nickname) {
        this.socialId = socialId;
        this.provider = provider;
        this.nickname = nickname;
    }

    public static Member create(String socialId, Provider provider, String nickname) {
        return Member.builder()
                .socialId(socialId)
                .provider(provider)
                .nickname(nickname)
                .build();
    }

    public enum Provider {
        KAKAO, GOOGLE, NAVER;

        // 외부의 Provider 문자열을 Provider 객체 타입으로 파싱
        public static Provider from(String providerName) {
            return Arrays.stream(Provider.values())
                    .filter(p -> p.name().equalsIgnoreCase(providerName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + providerName));
        }
    }
}
