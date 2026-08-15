package com.contextstt.backend.dto.auth;

import com.contextstt.backend.dto.user.UserResponse;
import lombok.Builder;

@Builder
public record LoginResponse(
        TokenResponse token,
        UserResponse user
) {
}
