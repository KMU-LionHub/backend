package com.contextstt.backend.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "대화 발언 연결 요청")
public record AddUtteranceRequest(
        @Schema(description = "연결할 전사 기록 ID", example = "21")
        @NotNull(message = "전사 기록 ID는 필수입니다.")
        @Positive(message = "전사 기록 ID는 양수여야 합니다.")
        Long transcriptionId,

        @Schema(description = "발화한 참여자 ID", example = "3")
        @NotNull(message = "발화자 ID는 필수입니다.")
        @Positive(message = "발화자 ID는 양수여야 합니다.")
        Long speakerParticipantId
) {
}
