package com.contextstt.backend.exception;

public class AudioTooLargeException extends RuntimeException {

    public AudioTooLargeException(long maxAudioBytes) {
        super("오디오 파일은 %d바이트 이하여야 합니다.".formatted(maxAudioBytes));
    }
}
