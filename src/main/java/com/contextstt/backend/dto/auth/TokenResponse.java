package com.contextstt.backend.dto.auth;

import lombok.Builder;

@Builder
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
