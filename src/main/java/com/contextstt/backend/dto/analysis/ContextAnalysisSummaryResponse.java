package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.domain.analysis.ContextAnalysis;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ContextAnalysisSummaryResponse(
        Long id,
        String provider,
        String model,
        int requestedCandidateCount,
        int ambiguityCount,
        int resolvedAmbiguityCount,
        boolean fullyResolved,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ContextAnalysisSummaryResponse from(ContextAnalysis analysis) {
        return ContextAnalysisSummaryResponse.builder()
                .id(analysis.getId())
                .provider(analysis.getProvider())
                .model(analysis.getModel())
                .requestedCandidateCount(analysis.getRequestedCandidateCount())
                .ambiguityCount(analysis.getAmbiguityCount())
                .resolvedAmbiguityCount(analysis.getResolvedAmbiguityCount())
                .fullyResolved(analysis.isFullyResolved())
                .createdAt(analysis.getCreatedAt())
                .updatedAt(analysis.getUpdatedAt())
                .build();
    }
}
