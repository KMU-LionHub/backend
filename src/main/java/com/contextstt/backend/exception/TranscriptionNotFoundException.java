package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class TranscriptionNotFoundException extends EntityNotFoundException {

    public TranscriptionNotFoundException() {
        super("전사 기록을 찾을 수 없습니다.");
    }
}
