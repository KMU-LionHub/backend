package com.contextstt.backend.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "재발언 전사 교체 요청")
public record ReplaceUtteranceTranscriptionRequest(
        @Schema(description = "재발언으로 새로 생성한 전사 기록 ID", example = "22")
        @NotNull(message = "전사 기록 ID는 필수입니다.")
        @Positive(message = "전사 기록 ID는 양수여야 합니다.")
        Long transcriptionId
) {
}
