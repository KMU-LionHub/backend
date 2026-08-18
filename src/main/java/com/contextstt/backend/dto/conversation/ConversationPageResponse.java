package com.contextstt.backend.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Schema(description = "대화 목록")
@Builder
public record ConversationPageResponse(
        List<ConversationSummaryResponse> conversations,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ConversationPageResponse from(Page<ConversationSummaryResponse> conversations) {
        return ConversationPageResponse.builder()
                .conversations(conversations.getContent())
                .page(conversations.getNumber())
                .size(conversations.getSize())
                .totalElements(conversations.getTotalElements())
                .totalPages(conversations.getTotalPages())
                .build();
    }
}
