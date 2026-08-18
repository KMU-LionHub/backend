package com.contextstt.backend.stt.google;

import com.contextstt.backend.exception.InvalidAudioException;
import com.contextstt.backend.exception.SpeechNotDetectedException;
import com.contextstt.backend.exception.SpeechProviderUnavailableException;
import com.contextstt.backend.stt.RecognizedWord;
import com.contextstt.backend.stt.SpeechRecognitionResult;
import com.contextstt.backend.stt.SpeechToTextGateway;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.speech.v2.AutoDetectDecodingConfig;
import com.google.cloud.speech.v2.RecognitionConfig;
import com.google.cloud.speech.v2.RecognitionFeatures;
import com.google.cloud.speech.v2.RecognizeRequest;
import com.google.cloud.speech.v2.RecognizeResponse;
import com.google.cloud.speech.v2.SpeechClient;
import com.google.cloud.speech.v2.SpeechRecognitionAlternative;
import com.google.cloud.speech.v2.SpeechSettings;
import com.google.cloud.speech.v2.WordInfo;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class GoogleSpeechToTextGateway implements SpeechToTextGateway {

    private static final String PROVIDER = "GOOGLE_SPEECH_V2";

    private final GoogleSttProperties properties;
    private final Object clientMonitor = new Object();

    private volatile SpeechClient speechClient;

    @Override
    public SpeechRecognitionResult recognize(byte[] audio, String languageCode) {
        ensureConfigured();

        try {
            return mapResponse(client().recognize(createRequest(audio, languageCode)));
        } catch (ApiException ex) {
            throw mapProviderException(ex);
        }
    }

    RecognizeRequest createRequest(byte[] audio, String languageCode) {

        RecognitionFeatures features = RecognitionFeatures.newBuilder()
                .setEnableAutomaticPunctuation(true)
                .setEnableWordTimeOffsets(true)
                .setEnableWordConfidence(true)
                .build();

        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build())
                .addLanguageCodes(languageCode)
                .setModel(model())
                .setFeatures(features)
                .build();

        return RecognizeRequest.newBuilder()
                .setRecognizer(properties.recognizerName())
                .setConfig(config)
                .setContent(ByteString.copyFrom(audio))
                .build();
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public String model() {
        return properties.getModel().trim();
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new SpeechProviderUnavailableException("STT 기능이 활성화되어 있지 않습니다.");
        }
        if (!StringUtils.hasText(properties.getProjectId())) {
            throw new SpeechProviderUnavailableException("Google Cloud 프로젝트 설정이 필요합니다.");
        }
    }

    private SpeechClient client() {
        SpeechClient current = speechClient;
        if (current != null) {
            return current;
        }

        synchronized (clientMonitor) {
            if (speechClient == null) {
                speechClient = createClient();
            }
            return speechClient;
        }
    }

    private SpeechClient createClient() {
        try {
            SpeechSettings settings = SpeechSettings.newBuilder()
                    .setEndpoint(properties.apiEndpoint())
                    .build();
            return SpeechClient.create(settings);
        } catch (IOException ex) {
            throw new SpeechProviderUnavailableException("STT 제공자에 연결할 수 없습니다.", ex);
        }
    }

    static SpeechRecognitionResult mapResponse(RecognizeResponse response) {
        List<String> transcripts = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        List<RecognizedWord> words = new ArrayList<>();

        response.getResultsList().forEach(result -> {
            if (result.getAlternativesCount() == 0) {
                return;
            }

            SpeechRecognitionAlternative alternative = result.getAlternatives(0);
            if (StringUtils.hasText(alternative.getTranscript())) {
                transcripts.add(alternative.getTranscript().trim());
            }
            if (alternative.getConfidence() > 0.0F) {
                confidences.add(alternative.getConfidence());
            }
            alternative.getWordsList().stream()
                    .map(GoogleSpeechToTextGateway::mapWord)
                    .forEach(words::add);
        });

        String transcript = String.join(" ", transcripts).trim();
        if (!StringUtils.hasText(transcript)) {
            throw new SpeechNotDetectedException();
        }

        Float confidence = confidences.isEmpty()
                ? null
                : (float) confidences.stream().mapToDouble(Float::doubleValue).average().orElseThrow();

        return new SpeechRecognitionResult(transcript, confidence, words);
    }

    private static RecognizedWord mapWord(WordInfo word) {
        return new RecognizedWord(
                word.getWord(),
                word.hasStartOffset() ? toMillis(word.getStartOffset()) : null,
                word.hasEndOffset() ? toMillis(word.getEndOffset()) : null,
                word.getConfidence() > 0.0F ? word.getConfidence() : null,
                StringUtils.hasText(word.getSpeakerLabel()) ? word.getSpeakerLabel() : null
        );
    }

    private static long toMillis(Duration duration) {
        return Math.max(0L, duration.getSeconds() * 1_000L + duration.getNanos() / 1_000_000L);
    }

    private RuntimeException mapProviderException(ApiException ex) {
        StatusCode.Code code = ex.getStatusCode().getCode();
        if (code == StatusCode.Code.INVALID_ARGUMENT) {
            return new InvalidAudioException(
                    "지원하지 않는 오디오이거나 60초 제한을 초과했습니다.",
                    ex
            );
        }
        return new SpeechProviderUnavailableException("STT 처리 중 일시적인 오류가 발생했습니다.", ex);
    }

    @PreDestroy
    void closeClient() {
        SpeechClient current = speechClient;
        speechClient = null;
        if (Objects.nonNull(current)) {
            current.close();
        }
    }
}
