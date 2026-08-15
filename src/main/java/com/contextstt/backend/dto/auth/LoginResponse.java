package com.contextstt.backend.dto.auth;

import com.contextstt.backend.dto.user.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "로그인 응답")
@Builder
public record LoginResponse(
        @Schema(description = "발급된 JWT 정보")
        TokenResponse token,
        @Schema(description = "로그인한 사용자")
        UserResponse user
) {
}