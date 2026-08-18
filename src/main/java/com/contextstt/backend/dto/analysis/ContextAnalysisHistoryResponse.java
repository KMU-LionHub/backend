package com.contextstt.backend.dto.analysis;

import java.util.List;
import lombok.Builder;

@Builder
public record ContextAnalysisHistoryResponse(
        Long conversationId,
        Long utteranceId,
        List<ContextAnalysisSummaryResponse> analyses
) {
}
