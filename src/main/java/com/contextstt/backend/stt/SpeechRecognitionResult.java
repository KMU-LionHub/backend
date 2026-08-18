package com.contextstt.backend.stt;

import java.util.List;

public record SpeechRecognitionResult(
        String transcript,
        Float confidence,
        List<RecognizedWord> words
) {

    public SpeechRecognitionResult {
        words = List.copyOf(words);
    }
}
