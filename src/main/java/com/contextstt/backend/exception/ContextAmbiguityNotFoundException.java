package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class ContextAmbiguityNotFoundException extends EntityNotFoundException {

    public ContextAmbiguityNotFoundException() {
        super("맥락 모호성 구간을 찾을 수 없습니다.");
    }
}
