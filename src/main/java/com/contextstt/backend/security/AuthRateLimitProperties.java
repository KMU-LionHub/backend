package com.contextstt.backend.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.rate-limit")
public record AuthRateLimitProperties(
        @Min(value = 1, message = "추적 가능한 요청 제한 키 수는 1 이상이어야 합니다.")
        long maximumTrackedKeys,
        @Valid @NotNull
        Policy signup,
        @Valid @NotNull
        Policy login
) {

    Policy policyFor(AuthEndpoint endpoint) {
        return switch (endpoint) {
            case SIGNUP -> signup;
            case LOGIN -> login;
        };
    }

    public record Policy(
            @Min(value = 1, message = "요청 제한 횟수는 1 이상이어야 합니다.")
            int capacity,
            @NotNull
            Duration window
    ) {

        @AssertTrue(message = "요청 제한 시간은 0보다 커야 합니다.")
        public boolean isWindowPositive() {
            return window != null && !window.isZero() && !window.isNegative();
        }
    }
}