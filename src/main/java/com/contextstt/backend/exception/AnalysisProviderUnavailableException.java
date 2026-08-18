package com.contextstt.backend.exception;

public class AnalysisProviderUnavailableException extends RuntimeException {

    public AnalysisProviderUnavailableException(String message) {
        super(message);
    }

    public AnalysisProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
