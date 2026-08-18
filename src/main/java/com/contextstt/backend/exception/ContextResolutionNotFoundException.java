package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class ContextResolutionNotFoundException extends EntityNotFoundException {

    public ContextResolutionNotFoundException() {
        super("현재 발언에 사용할 수 있는 확정 맥락을 찾을 수 없습니다.");
    }
}
