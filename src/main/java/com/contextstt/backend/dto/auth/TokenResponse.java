package com.contextstt.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "JWT 발급 정보")
@Builder
public record TokenResponse(
        @Schema(description = "JWT 액세스 토큰")
        String accessToken,
        @Schema(description = "인증 스킴", example = "Bearer")
        String tokenType,
        @Schema(description = "토큰 만료까지 남은 시간(초)", example = "3600")
        long expiresInSeconds
) {
}