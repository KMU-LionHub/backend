package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.domain.analysis.ContextAnalysisSelection;
import com.contextstt.backend.domain.analysis.ContextCandidate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Builder;

@Schema(description = "순위가 부여된 맥락 후보")
@Builder
public record ContextCandidateResponse(
        Long id,
        int rank,
        String interpretation,
        String inferredIntent,
        String rationale,
        @Schema(description = "후보 간 비교용 의도 유사도이며 확률이 아닙니다.", example = "0.8700")
        BigDecimal intentSimilarityScore,
        boolean selected
) {

    public static ContextCandidateResponse from(
            ContextCandidate candidate,
            ContextAnalysisSelection selection
    ) {
        boolean selected = selection != null
                && selection.getCandidate().getId().equals(candidate.getId());
        return ContextCandidateResponse.builder()
                .id(candidate.getId())
                .rank(candidate.getCandidateRank())
                .interpretation(candidate.getInterpretation())
                .inferredIntent(candidate.getInferredIntent())
                .rationale(candidate.getRationale())
                .intentSimilarityScore(candidate.getIntentSimilarityScore())
                .selected(selected)
                .build();
    }
}
