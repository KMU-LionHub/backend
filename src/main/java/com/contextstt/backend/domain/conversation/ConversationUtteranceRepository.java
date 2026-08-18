package com.contextstt.backend.domain.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationUtteranceRepository extends JpaRepository<ConversationUtterance, Long> {

    boolean existsByTranscriptionId(Long transcriptionId);
}
