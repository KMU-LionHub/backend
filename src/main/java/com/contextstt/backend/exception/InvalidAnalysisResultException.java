package com.contextstt.backend.exception;

public class InvalidAnalysisResultException extends RuntimeException {

    public InvalidAnalysisResultException(String message) {
        super(message);
    }

    public InvalidAnalysisResultException(String message, Throwable cause) {
        super(message, cause);
    }
}
