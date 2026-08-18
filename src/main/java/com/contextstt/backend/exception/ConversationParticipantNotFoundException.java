package com.contextstt.backend.exception;

import jakarta.persistence.EntityNotFoundException;

public class ConversationParticipantNotFoundException extends EntityNotFoundException {

    public ConversationParticipantNotFoundException() {
        super("대화 참여자를 찾을 수 없습니다.");
    }
}
