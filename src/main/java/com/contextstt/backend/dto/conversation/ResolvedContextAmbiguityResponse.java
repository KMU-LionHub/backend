package com.contextstt.backend.dto.conversation;

import com.contextstt.backend.domain.analysis.ContextAmbiguity;
import com.contextstt.backend.dto.analysis.ContextAnalysisSelectionResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "화자가 확정한 모호성 구간의 최종 맥락")
@Builder
public record ResolvedContextAmbiguityResponse(
        Long ambiguityId,
        int order,
        String excerpt,
        Long startWordId,
        Long endWordId,
        Integer startWordOrder,
        Integer endWordOrder,
        ContextAnalysisSelectionResponse resolution
) {

    public static ResolvedContextAmbiguityResponse from(ContextAmbiguity ambiguity) {
        return ResolvedContextAmbiguityResponse.builder()
                .ambiguityId(ambiguity.getId())
                .order(ambiguity.getAmbiguityOrder())
                .excerpt(ambiguity.getExcerpt())
                .startWordId(ambiguity.getStartWordId())
                .endWordId(ambiguity.getEndWordId())
                .startWordOrder(ambiguity.getStartWordOrder())
                .endWordOrder(ambiguity.getEndWordOrder())
                .resolution(ContextAnalysisSelectionResponse.from(ambiguity.getSelection()))
                .build();
    }
}
