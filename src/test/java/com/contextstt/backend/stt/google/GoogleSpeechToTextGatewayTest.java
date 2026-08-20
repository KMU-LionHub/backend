package com.contextstt.backend.stt.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contextstt.backend.exception.SpeechProviderUnavailableException;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.contextstt.backend.exception.SpeechNotDetectedException;
import com.contextstt.backend.stt.SpeechRecognitionResult;
import com.google.cloud.speech.v2.RecognizeResponse;
import com.google.cloud.speech.v2.SpeechRecognitionAlternative;
import com.google.cloud.speech.v2.WordInfo;
import com.google.protobuf.Duration;
import org.junit.jupiter.api.Test;

class GoogleSpeechToTextGatewayTest {

    @Test
    void mapsTopAlternativesAndWordMetadataWithoutLosingSegments() {
        WordInfo firstWord = WordInfo.newBuilder()
                .setWord("안녕하세요")
                .setStartOffset(duration(0, 120_000_000))
                .setEndOffset(duration(0, 840_000_000))
                .setConfidence(0.91F)
                .setSpeakerLabel("1")
                .build();
        WordInfo secondWord = WordInfo.newBuilder()
                .setWord("반갑습니다")
                .setStartOffset(duration(1, 0))
                .setEndOffset(duration(1, 650_000_000))
                .build();

        RecognizeResponse response = RecognizeResponse.newBuilder()
                .addResults(com.google.cloud.speech.v2.SpeechRecognitionResult.newBuilder()
                        .addAlternatives(SpeechRecognitionAlternative.newBuilder()
                                .setTranscript("안녕하세요")
                                .setConfidence(0.8F)
                                .addWords(firstWord)))
                .addResults(com.google.cloud.speech.v2.SpeechRecognitionResult.newBuilder()
                        .addAlternatives(SpeechRecognitionAlternative.newBuilder()
                                .setTranscript("반갑습니다")
                                .setConfidence(0.9F)
                                .addWords(secondWord)))
                .build();

        SpeechRecognitionResult result = GoogleSpeechToTextGateway.mapResponse(response);

        assertThat(result.transcript()).isEqualTo("안녕하세요 반갑습니다");
        assertThat(result.confidence()).isCloseTo(0.85F, offset(0.0001F));
        assertThat(result.words()).hasSize(2);
        assertThat(result.words().getFirst().startOffsetMillis()).isEqualTo(120L);
        assertThat(result.words().getFirst().endOffsetMillis()).isEqualTo(840L);
        assertThat(result.words().getFirst().confidence()).isEqualTo(0.91F);
        assertThat(result.words().getFirst().speakerLabel()).isEqualTo("1");
        assertThat(result.words().get(1).startOffsetMillis()).isEqualTo(1_000L);
        assertThat(result.words().get(1).confidence()).isNull();
    }

    @Test
    void rejectsResponseWithoutRecognizedSpeech() {
        RecognizeResponse response = RecognizeResponse.newBuilder()
                .addResults(com.google.cloud.speech.v2.SpeechRecognitionResult.newBuilder()
                        .addAlternatives(SpeechRecognitionAlternative.newBuilder()
                                .setTranscript("   ")))
                .build();

        assertThatThrownBy(() -> GoogleSpeechToTextGateway.mapResponse(response))
                .isInstanceOf(SpeechNotDetectedException.class)
                .hasMessage("음성에서 발화를 인식하지 못했습니다. 다시 녹음해 주세요.");
    }

    @Test
    void buildsRegionalAndGlobalEndpoints() {
        GoogleSttProperties properties = new GoogleSttProperties();
        properties.setProjectId("context-stt");

        assertThat(properties.recognizerName())
                .isEqualTo("projects/context-stt/locations/asia-northeast1/recognizers/_");
        assertThat(properties.apiEndpoint()).isEqualTo("asia-northeast1-speech.googleapis.com:443");

        properties.setLocation("global");

        assertThat(properties.apiEndpoint()).isEqualTo("speech.googleapis.com:443");
    }

    @Test
    void buildsV2RequestForEditableWordResults() {
        GoogleSttProperties properties = new GoogleSttProperties();
        properties.setEnabled(true);
        properties.setProjectId("context-stt");
        properties.setModel("long");
        GoogleSpeechToTextGateway gateway = new GoogleSpeechToTextGateway(
                properties,
                new GoogleAdcValidator(() -> null)
        );

        var request = gateway.createRequest(new byte[]{1, 2, 3}, "ko-KR");

        assertThat(request.getRecognizer())
                .isEqualTo("projects/context-stt/locations/asia-northeast1/recognizers/_");
        assertThat(request.getContent().toByteArray()).containsExactly(1, 2, 3);
        assertThat(request.getConfig().getLanguageCodesList()).containsExactly("ko-KR");
        assertThat(request.getConfig().getModel()).isEqualTo("long");
        assertThat(request.getConfig().hasAutoDecodingConfig()).isTrue();
        assertThat(request.getConfig().getFeatures().getEnableAutomaticPunctuation()).isTrue();
        assertThat(request.getConfig().getFeatures().getEnableWordTimeOffsets()).isTrue();
        assertThat(request.getConfig().getFeatures().getEnableWordConfidence()).isTrue();
    }

    @Test
    void mapsAuthenticationAndPermissionFailuresToActionableMessages() {
        GoogleSttProperties properties = new GoogleSttProperties();
        GoogleSpeechToTextGateway gateway = new GoogleSpeechToTextGateway(
                properties,
                new GoogleAdcValidator(() -> null)
        );

        assertThatThrownBy(() -> {
            throw gateway.mapProviderException(apiException(StatusCode.Code.UNAUTHENTICATED));
        })
                .isInstanceOf(SpeechProviderUnavailableException.class)
                .hasMessageContaining("ADC 자격증명");

        assertThatThrownBy(() -> {
            throw gateway.mapProviderException(apiException(StatusCode.Code.PERMISSION_DENIED));
        })
                .isInstanceOf(SpeechProviderUnavailableException.class)
                .hasMessageContaining("roles/speech.client");
    }

    @Test
    void sanitizesAndBoundsProviderMessagesBeforeLogging() {
        assertThat(GoogleSpeechToTextGateway.sanitizeProviderMessage(null))
                .isEqualTo("(no provider message)");
        assertThat(GoogleSpeechToTextGateway.sanitizeProviderMessage("first\nsecond\r\tthird"))
                .isEqualTo("first second  third");
        assertThat(GoogleSpeechToTextGateway.sanitizeProviderMessage("a".repeat(1_001)))
                .hasSize(1_003)
                .endsWith("...");
    }

    private ApiException apiException(StatusCode.Code code) {
        ApiException exception = mock(ApiException.class);
        StatusCode statusCode = mock(StatusCode.class);
        when(exception.getStatusCode()).thenReturn(statusCode);
        when(statusCode.getCode()).thenReturn(code);
        return exception;
    }

    private Duration duration(long seconds, int nanos) {
        return Duration.newBuilder().setSeconds(seconds).setNanos(nanos).build();
    }
}
