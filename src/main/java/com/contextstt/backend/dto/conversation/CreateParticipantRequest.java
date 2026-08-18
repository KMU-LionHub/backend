package com.contextstt.backend.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "상대 참여자 등록 요청")
public record CreateParticipantRequest(
        @Schema(description = "화면에 표시할 참여자 이름", example = "민수")
        @NotBlank(message = "참여자 이름은 필수입니다.")
        @Size(max = 50, message = "참여자 이름은 50자 이하여야 합니다.")
        String displayName
) {
}
