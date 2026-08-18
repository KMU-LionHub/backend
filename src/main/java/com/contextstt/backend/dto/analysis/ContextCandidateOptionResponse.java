package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.analysis.ContextCandidateOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "맥락 후보 항목")
@Builder
public record ContextCandidateOptionResponse(
        @Schema(description = "후보 맥락 설명")
        String content,
        @Schema(description = "신뢰도(0.0~1.0)", example = "0.8", nullable = true)
        Double confidence
) {

    public static ContextCandidateOptionResponse from(ContextCandidateOption option) {
        return ContextCandidateOptionResponse.builder()
                .content(option.content())
                .confidence(option.confidence())
                .build();
    }
}
