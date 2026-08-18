package com.contextstt.backend.analysis;

import com.contextstt.backend.domain.conversation.Conversation;
import com.contextstt.backend.domain.conversation.ConversationUtterance;
import com.contextstt.backend.domain.transcription.Transcription;

public record ContextAnalysisSource(
        Conversation conversation,
        ConversationUtterance utterance,
        Transcription transcription,
        ContextAnalysisInput input
) {
}
