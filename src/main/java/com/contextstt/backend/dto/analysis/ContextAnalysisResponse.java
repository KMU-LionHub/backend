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
        int candidateCount,
        List<ContextCandidateResponse> candidates,
        ContextAnalysisSelectionResponse selection,
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
                .candidateCount(analysis.getCandidateCount())
                .candidates(analysis.getCandidates().stream()
                        .map(candidate -> ContextCandidateResponse.from(candidate, analysis.getSelection()))
                        .toList())
                .selection(ContextAnalysisSelectionResponse.from(analysis.getSelection()))
                .createdAt(analysis.getCreatedAt())
                .updatedAt(analysis.getUpdatedAt())
                .build();
    }
}
