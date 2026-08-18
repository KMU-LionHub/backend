package com.contextstt.backend.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "AI 맥락 분석 요청")
public record AnalysisSubmitRequest(
        @Schema(description = "분석할 전사 기록 ID", example = "21")
        @NotNull
        Long transcriptionId
) {
}
