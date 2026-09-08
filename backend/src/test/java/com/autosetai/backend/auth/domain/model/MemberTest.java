package com.autosetai.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.autosetai.backend.auth.domain.model.Member.Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {
    @Test
    @DisplayName("Member 객체가 팩토리 메서드를 통해 정상적으로 생성된다")
    void create_Success() {
        // given
        String socialId = "kakao_123456789";
        Provider provider = Provider.KAKAO;
        String nickname = "득근득근_1234";

        // when
        Member member = Member.create(socialId, provider, nickname);

        // then
        assertThat(member.getSocialId()).isEqualTo(socialId);
        assertThat(member.getProvider()).isEqualTo(provider);
        assertThat(member.getNickname()).isEqualTo(nickname);
        assertThat(member.getId()).isNull(); // DB 저장 전이므로 PK는 null이어야 함
    }

    @Test
    @DisplayName("JPA 리플렉션을 위한 protected 기본 생성자가 정상 동작한다")
    void noArgsConstructor_Success() {
        // given & when
        // 테스트 클래스가 같은 패키지에 있으므로 protected 생성자 접근 가능
        Member member = new Member();

        // then
        assertThat(member).isNotNull();
        assertThat(member.getId()).isNull();
        assertThat(member.getSocialId()).isNull();
    }

    @Test
    @DisplayName("소셜 로그인 문자열(대소문자 무관)을 Provider Enum으로 정확히 변환한다")
    void providerFrom_Success() {
        // given
        String kakaoUpper = "KAKAO";
        String kakaoLower = "kakao";
        String googleMixed = "GoOgLe";

        // when
        Provider parsedKakao1 = Provider.from(kakaoUpper);
        Provider parsedKakao2 = Provider.from(kakaoLower);
        Provider parsedGoogle = Provider.from(googleMixed);

        // then
        assertThat(parsedKakao1).isEqualTo(Provider.KAKAO);
        assertThat(parsedKakao2).isEqualTo(Provider.KAKAO);
        assertThat(parsedGoogle).isEqualTo(Provider.GOOGLE);
    }

    @Test
    @DisplayName("지원하지 않는 소셜 로그인 문자열을 전달하면 예외가 발생한다")
    void providerFrom_Fail_InvalidProvider() {
        // given
        String invalidProviderName = "APPLE";

        // when & then
        assertThatThrownBy(() -> Member.Provider.from(invalidProviderName))
                .isInstanceOf(IllegalArgumentException.class);
    }
}