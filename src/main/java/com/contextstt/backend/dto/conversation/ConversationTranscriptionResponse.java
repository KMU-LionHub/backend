package com.contextstt.backend.dto.conversation;

import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.domain.transcription.TranscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Schema(description = "대화에 연결된 전사 요약")
@Builder
public record ConversationTranscriptionResponse(
        Long id,
        Long replacesTranscriptionId,
        String originalText,
        String currentText,
        String languageCode,
        TranscriptionStatus status,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ConversationTranscriptionResponse from(Transcription transcription) {
        return ConversationTranscriptionResponse.builder()
                .id(transcription.getId())
                .replacesTranscriptionId(transcription.getReplacesTranscriptionId().orElse(null))
                .originalText(transcription.getOriginalText())
                .currentText(transcription.getCurrentText())
                .languageCode(transcription.getLanguageCode())
                .status(transcription.getStatus())
                .confirmedAt(transcription.getConfirmedAt())
                .createdAt(transcription.getCreatedAt())
                .updatedAt(transcription.getUpdatedAt())
                .build();
    }
}
