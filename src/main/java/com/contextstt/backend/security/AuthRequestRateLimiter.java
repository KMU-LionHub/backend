package com.contextstt.backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class AuthRequestRateLimiter {

    private final AuthRateLimitProperties properties;
    private final Cache<ClientKey, Window> windows;

    public AuthRequestRateLimiter(AuthRateLimitProperties properties) {
        this.properties = properties;
        this.windows = Caffeine.newBuilder()
                .maximumSize(properties.maximumTrackedKeys())
                .expireAfterAccess(longerWindow(properties))
                .build();
    }

    RateLimitDecision tryAcquire(AuthEndpoint endpoint, String clientAddress) {
        AuthRateLimitProperties.Policy policy = properties.policyFor(endpoint);
        ClientKey key = new ClientKey(endpoint, clientAddress);
        Instant now = Instant.now();
        AtomicReference<RateLimitDecision> decision = new AtomicReference<>();

        windows.asMap().compute(key, (ignored, currentWindow) ->
                updateWindow(currentWindow, policy, now, decision)
        );

        return decision.get();
    }

    private Window updateWindow(
            Window currentWindow,
            AuthRateLimitProperties.Policy policy,
            Instant now,
            AtomicReference<RateLimitDecision> decision
    ) {
        if (currentWindow == null || !now.isBefore(currentWindow.resetsAt())) {
            decision.set(RateLimitDecision.permit());
            return new Window(1, now.plus(policy.window()));
        }

        if (currentWindow.requestCount() >= policy.capacity()) {
            decision.set(RateLimitDecision.reject(retryAfterSeconds(now, currentWindow.resetsAt())));
            return currentWindow;
        }

        decision.set(RateLimitDecision.permit());
        return new Window(currentWindow.requestCount() + 1, currentWindow.resetsAt());
    }

    private long retryAfterSeconds(Instant now, Instant resetsAt) {
        long remainingMillis = Math.max(1, Duration.between(now, resetsAt).toMillis());
        return Math.max(1, (remainingMillis + 999) / 1000);
    }

    private static Duration longerWindow(AuthRateLimitProperties properties) {
        Duration signupWindow = properties.signup().window();
        Duration loginWindow = properties.login().window();
        return signupWindow.compareTo(loginWindow) >= 0 ? signupWindow : loginWindow;
    }

    record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

        private static RateLimitDecision permit() {
            return new RateLimitDecision(true, 0);
        }

        private static RateLimitDecision reject(long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }
    }

    private record ClientKey(AuthEndpoint endpoint, String clientAddress) {
    }

    private record Window(int requestCount, Instant resetsAt) {
    }
}