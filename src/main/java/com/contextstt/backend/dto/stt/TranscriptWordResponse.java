package com.contextstt.backend.dto.stt;

import com.contextstt.backend.domain.transcription.TranscriptWord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Schema(description = "단어 단위 전사 결과")
@Builder
public record TranscriptWordResponse(
        @Schema(description = "단어 ID", example = "12")
        Long id,
        @Schema(description = "0부터 시작하는 단어 순서", example = "0")
        int order,
        @Schema(description = "STT 원본 단어", example = "의사소퉁")
        String originalText,
        @Schema(description = "화자가 교정한 단어. 교정하지 않았다면 null", example = "의사소통", nullable = true)
        String correctedText,
        @Schema(description = "화면과 후속 분석에 사용할 현재 단어", example = "의사소통")
        String currentText,
        @Schema(description = "발화 시작 시점(ms)", example = "120", nullable = true)
        Long startOffsetMillis,
        @Schema(description = "발화 종료 시점(ms)", example = "640", nullable = true)
        Long endOffsetMillis,
        @Schema(description = "Google STT 단어 신뢰도", example = "0.91", nullable = true)
        Float confidence,
        @Schema(description = "화자 분리 사용 시 화자 라벨", nullable = true)
        String speakerLabel,
        @Schema(description = "마지막 교정 시각", nullable = true)
        LocalDateTime correctedAt
) {

    public static TranscriptWordResponse from(TranscriptWord word) {
        return TranscriptWordResponse.builder()
                .id(word.getId())
                .order(word.getWordOrder())
                .originalText(word.getOriginalText())
                .correctedText(word.getCorrectedText())
                .currentText(word.getCurrentText())
                .startOffsetMillis(word.getStartOffsetMillis())
                .endOffsetMillis(word.getEndOffsetMillis())
                .confidence(word.getConfidence())
                .speakerLabel(word.getSpeakerLabel())
                .correctedAt(word.getCorrectedAt())
                .build();
    }
}
