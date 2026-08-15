package com.contextstt.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AuthRequestRateLimiterTest {

    @Test
    void rejectsRequestsAfterCapacityIsExhausted() {
        AuthRequestRateLimiter rateLimiter = rateLimiter(2);

        assertThat(rateLimiter.tryAcquire(AuthEndpoint.LOGIN, "192.0.2.1").allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire(AuthEndpoint.LOGIN, "192.0.2.1").allowed()).isTrue();

        AuthRequestRateLimiter.RateLimitDecision rejected =
                rateLimiter.tryAcquire(AuthEndpoint.LOGIN, "192.0.2.1");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void concurrentRequestsCannotExceedCapacity() throws Exception {
        int capacity = 10;
        int requestCount = 100;
        AuthRequestRateLimiter rateLimiter = rateLimiter(capacity);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return rateLimiter.tryAcquire(AuthEndpoint.SIGNUP, "192.0.2.2").allowed();
                }));
            }

            start.countDown();
            long allowedCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    allowedCount++;
                }
            }

            assertThat(allowedCount).isEqualTo(capacity);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private AuthRequestRateLimiter rateLimiter(int capacity) {
        AuthRateLimitProperties.Policy policy =
                new AuthRateLimitProperties.Policy(capacity, Duration.ofMinutes(1));
        AuthRateLimitProperties properties = new AuthRateLimitProperties(100, policy, policy);
        return new AuthRequestRateLimiter(properties);
    }
}