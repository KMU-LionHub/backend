package com.contextstt.backend.analysis.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.contextstt.backend.analysis.guardrail.ContextAnalysisRequestRateLimiter.RateLimitDecision;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ContextAnalysisRequestRateLimiterTest {

    @Test
    void rejectsRequestsAfterPerUserCapacityIsExhausted() {
        ContextAnalysisGuardrailProperties properties = new ContextAnalysisGuardrailProperties();
        properties.getRateLimit().setCapacity(2);
        properties.getRateLimit().setWindow(Duration.ofMinutes(1));
        ContextAnalysisRequestRateLimiter limiter = new ContextAnalysisRequestRateLimiter(properties);

        assertThat(limiter.tryAcquire(1L).allowed()).isTrue();
        assertThat(limiter.tryAcquire(1L).allowed()).isTrue();

        RateLimitDecision rejected = limiter.tryAcquire(1L);
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isBetween(1L, 60L);
        assertThat(limiter.tryAcquire(2L).allowed()).isTrue();
    }
}
