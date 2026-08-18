package com.contextstt.backend.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "대화 생성 요청")
public record CreateConversationRequest(
        @Schema(description = "대화 제목", example = "여행 일정 조율")
        @NotBlank(message = "대화 제목은 필수입니다.")
        @Size(max = 100, message = "대화 제목은 100자 이하여야 합니다.")
        String title,

        @Schema(description = "분석에 참고할 대화 상황", example = "친구와 여름 휴가 일정을 정하는 대화")
        @Size(max = 2000, message = "대화 상황은 2000자 이하여야 합니다.")
        String context,

        @Schema(description = "대화 생성 시 함께 등록할 상대 참여자")
        @Size(max = 20, message = "상대 참여자는 최대 20명까지 등록할 수 있습니다.")
        List<@NotNull(message = "참여자 정보는 null일 수 없습니다.") @Valid CreateParticipantRequest> participants
) {
}
