package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class TranscriptWordNotFoundException extends EntityNotFoundException {

    public TranscriptWordNotFoundException() {
        super("해당 전사 기록에서 단어를 찾을 수 없습니다.");
    }
}
