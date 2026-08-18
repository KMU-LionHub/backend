package com.contextstt.backend.stt;

public interface SpeechToTextGateway {

    SpeechRecognitionResult recognize(byte[] audio, String languageCode);

    String provider();

    String model();
}
