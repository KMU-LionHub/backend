package com.contextstt.backend.analysis.guardrail;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class ContextAnalysisRequestRateLimiter {

    private final ContextAnalysisGuardrailProperties.RateLimit properties;
    private final Cache<Long, Window> windows;

    public ContextAnalysisRequestRateLimiter(ContextAnalysisGuardrailProperties properties) {
        this.properties = properties.getRateLimit();
        this.windows = Caffeine.newBuilder()
                .maximumSize(this.properties.getMaximumTrackedUsers())
                .expireAfterAccess(this.properties.getWindow())
                .build();
    }

    public RateLimitDecision tryAcquire(Long userId) {
        Instant now = Instant.now();
        AtomicReference<RateLimitDecision> decision = new AtomicReference<>();
        windows.asMap().compute(userId, (ignored, currentWindow) ->
                updateWindow(currentWindow, now, decision)
        );
        return decision.get();
    }

    private Window updateWindow(
            Window currentWindow,
            Instant now,
            AtomicReference<RateLimitDecision> decision
    ) {
        if (currentWindow == null || !now.isBefore(currentWindow.resetsAt())) {
            decision.set(RateLimitDecision.permit());
            return new Window(1, now.plus(properties.getWindow()));
        }
        if (currentWindow.requestCount() >= properties.getCapacity()) {
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

    public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

        private static RateLimitDecision permit() {
            return new RateLimitDecision(true, 0);
        }

        private static RateLimitDecision reject(long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }
    }

    private record Window(int requestCount, Instant resetsAt) {
    }
}
