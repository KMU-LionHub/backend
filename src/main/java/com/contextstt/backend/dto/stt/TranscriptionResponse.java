package com.contextstt.backend.dto.stt;

import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.domain.transcription.TranscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Schema(description = "STT 전사 기록")
@Builder
public record TranscriptionResponse(
        @Schema(description = "전사 기록 ID", example = "21")
        Long id,
        @Schema(description = "재발언으로 대체한 이전 전사 ID", example = "18", nullable = true)
        Long replacesTranscriptionId,
        @Schema(description = "STT가 반환한 변경 불가능한 원문")
        String originalText,
        @Schema(description = "화자 교정을 반영한 현재 문장")
        String currentText,
        @Schema(description = "BCP-47 언어 코드", example = "ko-KR")
        String languageCode,
        @Schema(description = "STT 제공자", example = "GOOGLE_SPEECH_V2")
        String provider,
        @Schema(description = "STT 모델", example = "long")
        String model,
        @Schema(description = "문장 전체 신뢰도", example = "0.94", nullable = true)
        Float confidence,
        @Schema(description = "전사 상태(DRAFT, CONFIRMED, SUPERSEDED)", example = "DRAFT")
        TranscriptionStatus status,
        @Schema(description = "화자가 전사를 확정한 시각", nullable = true)
        LocalDateTime confirmedAt,
        @Schema(description = "업로드 오디오 MIME 타입", example = "audio/webm", nullable = true)
        String audioContentType,
        @Schema(description = "업로드 오디오 크기(byte)", example = "482310")
        long audioSizeBytes,
        List<TranscriptWordResponse> words,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TranscriptionResponse from(Transcription transcription) {
        return TranscriptionResponse.builder()
                .id(transcription.getId())
                .replacesTranscriptionId(transcription.getReplacesTranscriptionId().orElse(null))
                .originalText(transcription.getOriginalText())
                .currentText(transcription.getCurrentText())
                .languageCode(transcription.getLanguageCode())
                .provider(transcription.getProvider())
                .model(transcription.getModel())
                .confidence(transcription.getConfidence())
                .status(transcription.getStatus())
                .confirmedAt(transcription.getConfirmedAt())
                .audioContentType(transcription.getAudioContentType())
                .audioSizeBytes(transcription.getAudioSizeBytes())
                .words(transcription.getWords().stream().map(TranscriptWordResponse::from).toList())
                .createdAt(transcription.getCreatedAt())
                .updatedAt(transcription.getUpdatedAt())
                .build();
    }
}
