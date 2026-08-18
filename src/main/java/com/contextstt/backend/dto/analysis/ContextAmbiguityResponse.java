package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.domain.analysis.ContextAmbiguity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "맥락이 불분명한 단어 구간과 후보 목록")
@Builder
public record ContextAmbiguityResponse(
        Long id,
        int order,
        String excerpt,
        Long startWordId,
        Long endWordId,
        Integer startWordOrder,
        Integer endWordOrder,
        int candidateCount,
        List<ContextCandidateResponse> candidates,
        ContextAnalysisSelectionResponse selection
) {

    public static ContextAmbiguityResponse from(ContextAmbiguity ambiguity) {
        return ContextAmbiguityResponse.builder()
                .id(ambiguity.getId())
                .order(ambiguity.getAmbiguityOrder())
                .excerpt(ambiguity.getExcerpt())
                .startWordId(ambiguity.getStartWordId())
                .endWordId(ambiguity.getEndWordId())
                .startWordOrder(ambiguity.getStartWordOrder())
                .endWordOrder(ambiguity.getEndWordOrder())
                .candidateCount(ambiguity.getCandidateCount())
                .candidates(ambiguity.getCandidates().stream()
                        .map(candidate -> ContextCandidateResponse.from(candidate, ambiguity.getSelection()))
                        .toList())
                .selection(ContextAnalysisSelectionResponse.from(ambiguity.getSelection()))
                .build();
    }
}
