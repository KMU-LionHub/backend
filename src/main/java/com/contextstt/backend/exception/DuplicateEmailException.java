package com.contextstt.backend.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super(message(email));
    }

    public DuplicateEmailException(String email, Throwable cause) {
        super(message(email), cause);
    }

    private static String message(String email) {
        return "이미 가입된 이메일입니다: " + email;
    }
}