package com.contextstt.backend.dto.conversation;

import com.contextstt.backend.domain.conversation.Conversation;
import com.contextstt.backend.domain.conversation.ConversationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Schema(description = "대화 목록 항목")
@Builder
public record ConversationSummaryResponse(
        Long id,
        String title,
        String context,
        ConversationStatus status,
        int utteranceCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ConversationSummaryResponse from(Conversation conversation) {
        return ConversationSummaryResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .context(conversation.getContext())
                .status(conversation.getStatus())
                .utteranceCount(conversation.getUtteranceCount())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
