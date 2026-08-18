package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class ContextCandidateNotFoundException extends EntityNotFoundException {

    public ContextCandidateNotFoundException() {
        super("맥락 후보를 찾을 수 없습니다.");
    }
}
