package com.contextstt.backend.exception;

public class SpeechNotDetectedException extends RuntimeException {

    public SpeechNotDetectedException() {
        super("음성에서 발화를 인식하지 못했습니다. 다시 녹음해 주세요.");
    }
}
