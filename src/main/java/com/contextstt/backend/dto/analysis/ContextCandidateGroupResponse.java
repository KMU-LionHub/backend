package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.analysis.ContextCandidateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "맥락 손실 위험 구간과 그 맥락 후보 목록")
@Builder
public record ContextCandidateGroupResponse(
        @Schema(description = "맥락이 불분명한 발언 구간")
        String excerpt,
        List<ContextCandidateOptionResponse> candidates
) {

    public static ContextCandidateGroupResponse from(ContextCandidateGroup group) {
        return ContextCandidateGroupResponse.builder()
                .excerpt(group.excerpt())
                .candidates(group.candidates().stream().map(ContextCandidateOptionResponse::from).toList())
                .build();
    }
}
