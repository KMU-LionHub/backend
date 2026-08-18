package com.contextstt.backend.dto.analysis;

import com.contextstt.backend.analysis.ContextAnalysisModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "맥락 분석 요청")
public record CreateContextAnalysisRequest(
        @Schema(description = "대화 ID", example = "5")
        @NotNull(message = "대화 ID는 필수입니다.")
        @Positive(message = "대화 ID는 양수여야 합니다.")
        Long conversationId,

        @Schema(description = "분석할 확정 발언 ID", example = "12")
        @NotNull(message = "발언 ID는 필수입니다.")
        @Positive(message = "발언 ID는 양수여야 합니다.")
        Long utteranceId,

        @Schema(description = "생성할 후보 수(기본 3, 최대 5)", example = "3")
        @Min(value = 2, message = "맥락 후보는 2개 이상 요청해야 합니다.")
        @Max(value = 5, message = "맥락 후보는 최대 5개까지 요청할 수 있습니다.")
        Integer candidateCount,

        @Schema(
                description = "분석 모델. 생략하면 CLAUDE_SONNET_5",
                example = "GEMINI_3_7_FLASH",
                allowableValues = {
                        "CLAUDE_SONNET_5",
                        "GEMINI_3_7_FLASH",
                        "DEEPSEEK_V4_FLASH"
                }
        )
        ContextAnalysisModel model
) {
}
