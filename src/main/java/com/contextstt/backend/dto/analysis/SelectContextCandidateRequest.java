package com.contextstt.backend.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "맥락 후보 선택 요청")
public record SelectContextCandidateRequest(
        @Schema(description = "선택할 후보 ID", example = "31")
        @NotNull(message = "맥락 후보 ID는 필수입니다.")
        @Positive(message = "맥락 후보 ID는 양수여야 합니다.")
        Long candidateId
) {
}
