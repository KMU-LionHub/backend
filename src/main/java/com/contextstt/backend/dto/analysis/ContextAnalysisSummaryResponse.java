package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.domain.analysis.ContextAnalysis;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ContextAnalysisSummaryResponse(
        Long id,
        String provider,
        String model,
        int candidateCount,
        String selectedContext,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ContextAnalysisSummaryResponse from(ContextAnalysis analysis) {
        String selectedContext = analysis.getSelection() == null
                ? null
                : analysis.getSelection().getFinalText();
        return ContextAnalysisSummaryResponse.builder()
                .id(analysis.getId())
                .provider(analysis.getProvider())
                .model(analysis.getModel())
                .candidateCount(analysis.getCandidateCount())
                .selectedContext(selectedContext)
                .createdAt(analysis.getCreatedAt())
                .updatedAt(analysis.getUpdatedAt())
                .build();
    }
}
