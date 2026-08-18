package com.contextstt.backend.exception;

public class SpeechProviderUnavailableException extends RuntimeException {

    public SpeechProviderUnavailableException(String message) {
        super(message);
    }

    public SpeechProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
