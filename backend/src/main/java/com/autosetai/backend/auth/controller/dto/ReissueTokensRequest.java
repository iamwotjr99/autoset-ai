package com.autosetai.backend.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueTokensRequest(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {

}
