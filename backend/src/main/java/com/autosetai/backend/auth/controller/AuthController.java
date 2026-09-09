package com.autosetai.backend.auth.controller;

import com.autosetai.backend.auth.controller.dto.ReissueTokensRequest;
import com.autosetai.backend.auth.controller.dto.SocialLoginRequest;
import com.autosetai.backend.auth.service.AuthService;
import com.autosetai.backend.auth.service.dto.AuthTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/social-login")
    public ResponseEntity<AuthTokenResponse> socialLogin(@RequestBody @Valid SocialLoginRequest req) {
        AuthTokenResponse result = authService.socialLogin(req.socialAccessToken());
        return ResponseEntity.ok(AuthTokenResponse.of(result.accessToken(), result.refreshToken()));
    }

    @PostMapping("/reissue-tokens")
    public ResponseEntity<AuthTokenResponse> reissueTokens(@RequestBody @Valid ReissueTokensRequest req) {
        AuthTokenResponse result = authService.reissueTokens(req.refreshToken());
        return ResponseEntity.ok(AuthTokenResponse.of(result.accessToken(), result.refreshToken()));
    }
}
