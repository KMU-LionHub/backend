package com.contextstt.backend.exception;

import lombok.Getter;

@Getter
public class ContextAnalysisRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public ContextAnalysisRateLimitExceededException(long retryAfterSeconds) {
        super("맥락 분석 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
