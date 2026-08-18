package com.contextstt.backend.dto.conversation;

import com.contextstt.backend.domain.conversation.Conversation;
import com.contextstt.backend.domain.conversation.ConversationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Schema(description = "대화 상세")
@Builder
public record ConversationResponse(
        Long id,
        String title,
        String context,
        ConversationStatus status,
        List<ConversationParticipantResponse> participants,
        List<ConversationUtteranceResponse> utterances,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ConversationResponse from(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .context(conversation.getContext())
                .status(conversation.getStatus())
                .participants(conversation.getParticipantsInOrder().stream()
                        .map(ConversationParticipantResponse::from)
                        .toList())
                .utterances(conversation.getUtterances().stream()
                        .map(ConversationUtteranceResponse::from)
                        .toList())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
