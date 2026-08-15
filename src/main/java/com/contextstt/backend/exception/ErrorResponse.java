package com.contextstt.backend.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Schema(description = "API 오류 응답")
@Builder
public record ErrorResponse(
        @Schema(description = "오류 발생 시각", example = "2026-08-15T23:00:00")
        LocalDateTime timestamp,
        @Schema(description = "HTTP 상태 코드", example = "400")
        int status,
        @Schema(description = "오류 메시지", example = "입력값이 올바르지 않습니다.")
        String message,
        @Schema(description = "필드별 상세 오류")
        List<String> errors
) {
}