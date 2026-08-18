package com.contextstt.backend.dto.conversation;

import com.contextstt.backend.domain.conversation.ConversationUtterance;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Schema(description = "대화 발언")
@Builder
public record ConversationUtteranceResponse(
        Long id,
        int order,
        ConversationParticipantResponse speaker,
        ConversationTranscriptionResponse transcription,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ConversationUtteranceResponse from(ConversationUtterance utterance) {
        return ConversationUtteranceResponse.builder()
                .id(utterance.getId())
                .order(utterance.getUtteranceOrder())
                .speaker(ConversationParticipantResponse.from(utterance.getSpeaker()))
                .transcription(ConversationTranscriptionResponse.from(utterance.getTranscription()))
                .createdAt(utterance.getCreatedAt())
                .updatedAt(utterance.getUpdatedAt())
                .build();
    }
}
