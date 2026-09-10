package com.autosetai.backend.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

// 추후 소셜 로그인 제공자가 카카오뿐만 아니라 추가가 된다면 Provider를 변수로 받아야함
public record SocialLoginRequest(
        @NotBlank(message = "소셜 액세스 토큰은 필수입니다.")
        String socialAccessToken
) {

}
