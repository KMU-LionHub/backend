package com.contextstt.backend.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "선택 맥락 수정 요청")
public record EditContextSelectionRequest(
        @Schema(description = "화자가 직접 확정한 맥락", example = "일정을 거절하려는 것이 아니라 시간을 다시 확인하려는 뜻")
        @NotBlank(message = "확정 맥락은 필수입니다.")
        @Size(max = 4000, message = "확정 맥락은 4000자 이하여야 합니다.")
        String text
) {
}
