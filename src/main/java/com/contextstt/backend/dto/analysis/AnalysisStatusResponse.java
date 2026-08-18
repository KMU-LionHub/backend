package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.domain.analysis.Analysis;
import com.contextstt.backend.domain.analysis.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import tools.jackson.databind.ObjectMapper;

@Schema(description = "AI 맥락 분석 상태")
@Builder
public record AnalysisStatusResponse(
        @Schema(description = "분석 ID", example = "7")
        Long id,
        @Schema(description = "분석 대상 전사 ID", example = "21")
        Long transcriptionId,
        @Schema(description = "분석 상태")
        AnalysisStatus status,
        @Schema(description = "분석 진행률(%)", example = "100")
        int progress,
        @Schema(description = "맥락 후보 목록. 분석이 끝나기 전에는 null", nullable = true)
        List<ContextCandidateGroupResponse> contextCandidates,
        @Schema(description = "실패 사유. 실패하지 않았으면 null", nullable = true)
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AnalysisStatusResponse from(Analysis analysis, ObjectMapper objectMapper) {
        List<ContextCandidateGroupResponse> contextCandidates = null;
        if (analysis.getStatus() == AnalysisStatus.COMPLETED && analysis.getResultJson() != null) {
            ContextAnalysisResult result = objectMapper.readValue(analysis.getResultJson(), ContextAnalysisResult.class);
            contextCandidates = result.contextCandidates().stream()
                    .map(ContextCandidateGroupResponse::from)
                    .toList();
        }

        return AnalysisStatusResponse.builder()
                .id(analysis.getId())
                .transcriptionId(analysis.getTranscription().getId())
                .status(analysis.getStatus())
                .progress(analysis.getProgress())
                .contextCandidates(contextCandidates)
                .errorMessage(analysis.getErrorMessage())
                .createdAt(analysis.getCreatedAt())
                .updatedAt(analysis.getUpdatedAt())
                .build();
    }
}
