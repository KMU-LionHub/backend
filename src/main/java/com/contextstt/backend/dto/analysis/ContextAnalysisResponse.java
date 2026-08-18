package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.domain.analysis.ContextAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Schema(description = "맥락 분석 상세 결과")
@Builder
public record ContextAnalysisResponse(
        Long id,
        Long conversationId,
        Long utteranceId,
        Long transcriptionId,
        String sourceSpeakerName,
        String conversationContext,
        String sourceOriginalText,
        String sourceCurrentText,
        String provider,
        String model,
        int requestedCandidateCount,
        int ambiguityCount,
        boolean needsClarification,
        boolean stale,
        boolean fullyResolved,
        boolean usableResolution,
        List<ContextAmbiguityResponse> ambiguities,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ContextAnalysisResponse from(ContextAnalysis analysis) {
        return ContextAnalysisResponse.builder()
                .id(analysis.getId())
                .conversationId(analysis.getConversation().getId())
                .utteranceId(analysis.getUtterance().getId())
                .transcriptionId(analysis.getTranscription().getId())
                .sourceSpeakerName(analysis.getSourceSpeakerName())
                .conversationContext(analysis.getConversationContext())
                .sourceOriginalText(analysis.getSourceOriginalText())
                .sourceCurrentText(analysis.getSourceCurrentText())
                .provider(analysis.getProvider())
                .model(analysis.getModel())
                .requestedCandidateCount(analysis.getRequestedCandidateCount())
                .ambiguityCount(analysis.getAmbiguityCount())
                .needsClarification(analysis.getAmbiguityCount() > 0)
                .stale(analysis.isStale())
                .fullyResolved(analysis.isFullyResolved())
                .usableResolution(analysis.hasUsableResolution())
                .ambiguities(analysis.getAmbiguities().stream()
                        .map(ContextAmbiguityResponse::from)
                        .toList())
                .createdAt(analysis.getCreatedAt())
                .updatedAt(analysis.getUpdatedAt())
                .build();
    }
}
