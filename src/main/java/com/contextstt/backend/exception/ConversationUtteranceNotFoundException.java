package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class ConversationUtteranceNotFoundException extends EntityNotFoundException {

    public ConversationUtteranceNotFoundException() {
        super("대화 발언을 찾을 수 없습니다.");
    }
}
