package com.contextstt.backend.exception;

public class ContextAnalysisProviderUnavailableException extends RuntimeException {

    public ContextAnalysisProviderUnavailableException(String message) {
        super(message);
    }

    public ContextAnalysisProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
