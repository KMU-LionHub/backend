package com.contextstt.backend.dto.stt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "단어 교정 요청")
public record WordCorrectionRequest(
        @Schema(description = "화자가 확정한 단어", example = "의사소통")
        @NotBlank(message = "교정 단어는 필수입니다.")
        @Size(max = 2000, message = "교정 단어는 2000자 이하여야 합니다.")
        String text
) {
}
