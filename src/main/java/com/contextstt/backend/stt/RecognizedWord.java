package com.contextstt.backend.stt;

public record RecognizedWord(
        String text,
        Long startOffsetMillis,
        Long endOffsetMillis,
        Float confidence,
        String speakerLabel
) {
}
