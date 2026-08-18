package com.contextstt.backend.dto.conversation;

import com.contextstt.backend.domain.analysis.ContextAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Schema(description = "대화 발언의 최신 유효 확정 맥락")
@Builder
public record ConversationUtteranceResolutionResponse(
        Long conversationId,
        Long utteranceId,
        Long transcriptionId,
        Long analysisId,
        String sourceCurrentText,
        boolean needsClarification,
        List<ResolvedContextAmbiguityResponse> resolvedAmbiguities,
        LocalDateTime analyzedAt,
        LocalDateTime resolvedAt
) {

    public static ConversationUtteranceResolutionResponse from(ContextAnalysis analysis) {
        return ConversationUtteranceResolutionResponse.builder()
                .conversationId(analysis.getConversation().getId())
                .utteranceId(analysis.getUtterance().getId())
                .transcriptionId(analysis.getTranscription().getId())
                .analysisId(analysis.getId())
                .sourceCurrentText(analysis.getSourceCurrentText())
                .needsClarification(analysis.getAmbiguityCount() > 0)
                .resolvedAmbiguities(analysis.getAmbiguities().stream()
                        .map(ResolvedContextAmbiguityResponse::from)
                        .toList())
                .analyzedAt(analysis.getCreatedAt())
                .resolvedAt(analysis.getUpdatedAt())
                .build();
    }
}
