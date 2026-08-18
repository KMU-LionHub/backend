package com.contextstt.backend.dto.conversation;

import com.contextstt.backend.domain.conversation.ConversationParticipant;
import com.contextstt.backend.domain.conversation.ParticipantType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Schema(description = "대화 참여자")
@Builder
public record ConversationParticipantResponse(
        Long id,
        Long userId,
        String displayName,
        ParticipantType type,
        LocalDateTime createdAt
) {

    public static ConversationParticipantResponse from(ConversationParticipant participant) {
        return ConversationParticipantResponse.builder()
                .id(participant.getId())
                .userId(participant.getUserId())
                .displayName(participant.getDisplayName())
                .type(participant.getType())
                .createdAt(participant.getCreatedAt())
                .build();
    }
}
