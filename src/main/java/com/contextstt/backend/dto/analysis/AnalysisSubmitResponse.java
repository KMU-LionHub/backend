package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.domain.analysis.Analysis;
import com.contextstt.backend.domain.analysis.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "AI 맥락 분석 요청 접수 결과")
@Builder
public record AnalysisSubmitResponse(
        @Schema(description = "분석 ID", example = "7")
        Long analysisId,
        @Schema(description = "분석 상태")
        AnalysisStatus status,
        @Schema(description = "분석 진행률(%)", example = "0")
        int progress
) {

    public static AnalysisSubmitResponse from(Analysis analysis) {
        return AnalysisSubmitResponse.builder()
                .analysisId(analysis.getId())
                .status(analysis.getStatus())
                .progress(analysis.getProgress())
                .build();
    }
}
