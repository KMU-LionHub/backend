package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class ConversationNotFoundException extends EntityNotFoundException {

    public ConversationNotFoundException() {
        super("대화를 찾을 수 없습니다.");
    }
}
