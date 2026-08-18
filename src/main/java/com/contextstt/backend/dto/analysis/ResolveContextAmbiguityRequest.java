package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.domain.analysis.ContextResolutionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "모호성 구간 확정 요청")
public record ResolveContextAmbiguityRequest(
        @NotNull(message = "확정 유형은 필수입니다.")
        ContextResolutionType type,

        @Positive(message = "맥락 후보 ID는 양수여야 합니다.")
        Long candidateId,

        @Size(max = 4000, message = "직접 입력한 맥락은 4000자 이하여야 합니다.")
        String text
) {
}
