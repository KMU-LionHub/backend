package com.contextstt.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contextstt.backend.analysis.ContextAnalysisGateway;
import com.contextstt.backend.analysis.ContextAnalysisInput;
import com.contextstt.backend.analysis.ContextAnalysisSource;
import com.contextstt.backend.analysis.ContextAnalysisSourceLoader;
import com.contextstt.backend.analysis.guardrail.ContextAnalysisRequestRateLimiter;
import com.contextstt.backend.analysis.guardrail.ContextAnalysisRequestRateLimiter.RateLimitDecision;
import com.contextstt.backend.domain.analysis.ContextAnalysisRepository;
import com.contextstt.backend.dto.analysis.CreateContextAnalysisRequest;
import com.contextstt.backend.exception.ContextAnalysisRateLimitExceededException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContextAnalysisServiceGuardrailTest {

    @Test
    void rejectsRateLimitedRequestBeforeCallingExternalProvider() {
        ContextAnalysisSourceLoader sourceLoader = mock(ContextAnalysisSourceLoader.class);
        ContextAnalysisGateway gateway = mock(ContextAnalysisGateway.class);
        ContextAnalysisRepository repository = mock(ContextAnalysisRepository.class);
        ContextAnalysisRequestRateLimiter rateLimiter = mock(ContextAnalysisRequestRateLimiter.class);
        ContextAnalysisInput input = new ContextAnalysisInput(
                1L,
                2L,
                null,
                List.of(),
                List.of(),
                0,
                "화자",
                "원문",
                "현재 문장",
                List.of(new ContextAnalysisInput.AnalysisWord(0, "현재 문장"))
        );
        when(sourceLoader.load(7L, 1L, 2L))
                .thenReturn(new ContextAnalysisSource(null, null, null, input));
        when(rateLimiter.tryAcquire(7L)).thenReturn(new RateLimitDecision(false, 30));
        ContextAnalysisService service = new ContextAnalysisService(
                sourceLoader,
                gateway,
                repository,
                rateLimiter
        );

        assertThatThrownBy(() -> service.analyze(
                7L,
                new CreateContextAnalysisRequest(1L, 2L, 3, null)
        ))
                .isInstanceOf(ContextAnalysisRateLimitExceededException.class)
                .extracting("retryAfterSeconds")
                .isEqualTo(30L);

        verifyNoInteractions(gateway);
    }
}
