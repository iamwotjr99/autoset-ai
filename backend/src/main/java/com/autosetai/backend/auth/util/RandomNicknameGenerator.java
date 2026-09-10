package com.autosetai.backend.auth.util;

import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class RandomNicknameGenerator {
    // 피트니스 관련 형용사 모음
    private static final List<String> ADJECTIVES = List.of(
            "득근하는", "건강한", "강력한", "날렵한", "꾸준한", "단단한", "지치지않는"
    );

    // 피트니스 관련 명사 모음
    private static final List<String> NOUNS = List.of(
            "헬린이", "덤벨", "바벨", "프로틴", "근육", "오운완", "스쿼트"
    );

    private final Random random = new Random();

    public String generate() {
        // 랜덤하게 형용사와 명사를 하나씩 뽑기
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));

        // 중복 방지를 위한 4자리 랜덤 숫자 (1000 ~ 9999)
        int randomNumber = 1000 + random.nextInt(9000);

        // 결과 예시: 득근하는_오운완_8241
        return adjective + "_" + noun + "_" + randomNumber;
    }
}
