package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class AnalysisNotFoundException extends EntityNotFoundException {

    public AnalysisNotFoundException() {
        super("분석 기록을 찾을 수 없습니다.");
    }
}
