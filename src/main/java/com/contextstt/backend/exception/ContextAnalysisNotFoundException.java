package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class ContextAnalysisNotFoundException extends EntityNotFoundException {

    public ContextAnalysisNotFoundException() {
        super("맥락 분석 결과를 찾을 수 없습니다.");
    }
}
