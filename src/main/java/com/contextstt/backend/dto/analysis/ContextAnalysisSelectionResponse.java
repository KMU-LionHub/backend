package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.domain.analysis.ContextAnalysisSelection;
import com.contextstt.backend.domain.analysis.ContextResolutionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Schema(description = "화자가 선택하거나 수정한 최종 맥락")
@Builder
public record ContextAnalysisSelectionResponse(
        ContextResolutionType type,
        Long candidateId,
        String originalCandidateText,
        String finalText,
        boolean edited,
        LocalDateTime selectedAt,
        LocalDateTime updatedAt
) {

    public static ContextAnalysisSelectionResponse from(ContextAnalysisSelection selection) {
        if (selection == null) {
            return null;
        }
        return ContextAnalysisSelectionResponse.builder()
                .type(selection.getResolutionType())
                .candidateId(selection.getCandidate() == null ? null : selection.getCandidate().getId())
                .originalCandidateText(selection.getOriginalCandidateText())
                .finalText(selection.getFinalText())
                .edited(selection.isEdited())
                .selectedAt(selection.getSelectedAt())
                .updatedAt(selection.getUpdatedAt())
                .build();
    }
}
