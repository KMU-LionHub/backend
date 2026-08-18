package com.contextstt.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.domain.user.UserRepository;
import com.contextstt.backend.exception.SpeechNotDetectedException;
import com.contextstt.backend.exception.SpeechProviderUnavailableException;
import com.contextstt.backend.security.JwtTokenProvider;
import com.contextstt.backend.stt.RecognizedWord;
import com.contextstt.backend.stt.SpeechRecognitionResult;
import com.contextstt.backend.stt.SpeechToTextGateway;
import com.contextstt.backend.stt.google.GoogleSttProperties;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SttControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private GoogleSttProperties sttProperties;

    @MockitoBean
    private SpeechToTextGateway speechToTextGateway;

    @BeforeEach
    void setUpGatewayMetadata() {
        when(speechToTextGateway.provider()).thenReturn("GOOGLE_SPEECH_V2");
        when(speechToTextGateway.model()).thenReturn("long");
    }

    @Test
    void transcribesAudioAndPersistsWordLevelMetadata() throws Exception {
        String token = accessToken();
        when(speechToTextGateway.recognize(any(byte[].class), eq("ko-KR")))
                .thenReturn(recognition("안녕 하세요", List.of(
                        new RecognizedWord("안녕", 0L, 350L, 0.82F, null),
                        new RecognizedWord("하세요", 360L, 910L, 0.94F, null)
                )));

        mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(audio("speech.webm", "audio/webm", new byte[]{1, 2, 3, 4}))
                        .param("languageCode", "ko-kr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.replacesTranscriptionId").doesNotExist())
                .andExpect(jsonPath("$.originalText").value("안녕 하세요"))
                .andExpect(jsonPath("$.currentText").value("안녕 하세요"))
                .andExpect(jsonPath("$.languageCode").value("ko-KR"))
                .andExpect(jsonPath("$.provider").value("GOOGLE_SPEECH_V2"))
                .andExpect(jsonPath("$.model").value("long"))
                .andExpect(jsonPath("$.audioContentType").value("audio/webm"))
                .andExpect(jsonPath("$.audioSizeBytes").value(4))
                .andExpect(jsonPath("$.words.length()").value(2))
                .andExpect(jsonPath("$.words[0].originalText").value("안녕"))
                .andExpect(jsonPath("$.words[0].currentText").value("안녕"))
                .andExpect(jsonPath("$.words[0].startOffsetMillis").value(0))
                .andExpect(jsonPath("$.words[0].endOffsetMillis").value(350));
    }

    @Test
    void correctsOneWordWhilePreservingOriginalTranscript() throws Exception {
        String token = accessToken();
        when(speechToTextGateway.recognize(any(byte[].class), eq("ko-KR")))
                .thenReturn(recognition("정보 손실", List.of(
                        new RecognizedWord("정보", 0L, 200L, 0.9F, null),
                        new RecognizedWord("손실", 210L, 500L, 0.6F, null)
                )));

        MvcResult created = createTranscription(token, "ko-KR");
        Number transcriptionId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        Number wordId = JsonPath.read(created.getResponse().getContentAsString(), "$.words[1].id");

        mockMvc.perform(patch("/api/stt/transcriptions/{transcriptionId}/words/{wordId}",
                        transcriptionId.longValue(), wordId.longValue())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"손씰\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalText").value("정보 손실"))
                .andExpect(jsonPath("$.currentText").value("정보 손씰"))
                .andExpect(jsonPath("$.words[1].originalText").value("손실"))
                .andExpect(jsonPath("$.words[1].correctedText").value("손씰"))
                .andExpect(jsonPath("$.words[1].currentText").value("손씰"))
                .andExpect(jsonPath("$.words[1].correctedAt").exists());

        mockMvc.perform(get("/api/stt/transcriptions/{transcriptionId}", transcriptionId.longValue())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalText").value("정보 손실"))
                .andExpect(jsonPath("$.currentText").value("정보 손씰"));
    }

    @Test
    void reRecordingCreatesReplacementWithoutDeletingOriginal() throws Exception {
        String token = accessToken();
        when(speechToTextGateway.recognize(any(byte[].class), eq("ko-KR")))
                .thenReturn(recognition("첫 발언", List.of()), recognition("다시 한 발언", List.of()));

        MvcResult original = createTranscription(token, "ko-KR");
        Number originalId = JsonPath.read(original.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(multipart("/api/stt/transcriptions/{transcriptionId}/re-record", originalId.longValue())
                        .file(audio("retry.wav", "audio/wav", new byte[]{5, 6, 7}))
                        .param("languageCode", "ko-KR")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(originalId.intValue())))
                .andExpect(jsonPath("$.replacesTranscriptionId").value(originalId.intValue()))
                .andExpect(jsonPath("$.originalText").value("다시 한 발언"))
                .andExpect(jsonPath("$.words.length()").value(3));

        mockMvc.perform(get("/api/stt/transcriptions/{transcriptionId}", originalId.longValue())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalText").value("첫 발언"));
    }

    @Test
    void hidesAnotherUsersTranscriptionAndWords() throws Exception {
        String ownerToken = accessToken();
        String otherToken = accessToken();
        when(speechToTextGateway.recognize(any(byte[].class), eq("ko-KR")))
                .thenReturn(recognition("비공개 발화", List.of()));

        MvcResult created = createTranscription(ownerToken, "ko-KR");
        Number transcriptionId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        Number wordId = JsonPath.read(created.getResponse().getContentAsString(), "$.words[0].id");

        mockMvc.perform(get("/api/stt/transcriptions/{transcriptionId}", transcriptionId.longValue())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/stt/transcriptions/{transcriptionId}/words/{wordId}",
                        transcriptionId.longValue(), wordId.longValue())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"노출\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesAuthenticationAudioLanguageAndSizeBeforeProviderCall() throws Exception {
        MockMultipartFile validAudio = audio("speech.webm", "audio/webm", new byte[]{1});

        mockMvc.perform(multipart("/api/stt/transcriptions").file(validAudio))
                .andExpect(status().isUnauthorized());

        String token = accessToken();
        mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(audio("image.png", "image/png", new byte[]{1}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("지원하지 않는 오디오 미디어 타입입니다: image/png"));

        mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(audio("speech.webm", "audio/webm", new byte[]{1}))
                        .param("languageCode", "not_a_locale")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(audio("too-large.webm", "audio/webm",
                                new byte[(int) sttProperties.getMaxAudioBytes() + 1]))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isContentTooLarge());

        verifyNoInteractions(speechToTextGateway);
    }

    @Test
    void mapsMissingAudioAndProviderFailuresToActionableStatuses() throws Exception {
        String token = accessToken();

        mockMvc.perform(multipart("/api/stt/transcriptions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필수 요청 값이 누락되었습니다."));

        when(speechToTextGateway.recognize(any(byte[].class), eq("ko-KR")))
                .thenThrow(new SpeechNotDetectedException())
                .thenThrow(new SpeechProviderUnavailableException("STT 제공자 장애"));

        mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(audio("silence.webm", "audio/webm", new byte[]{1}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message")
                        .value("음성에서 발화를 인식하지 못했습니다. 다시 녹음해 주세요."));

        mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(audio("speech.webm", "audio/webm", new byte[]{1}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("STT 제공자 장애"));
    }

    private MvcResult createTranscription(String token, String languageCode) throws Exception {
        return mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(audio("speech.webm", "audio/webm", new byte[]{1, 2, 3}))
                        .param("languageCode", languageCode)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MockMultipartFile audio(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("audio", filename, contentType, content);
    }

    private SpeechRecognitionResult recognition(String transcript, List<RecognizedWord> words) {
        return new SpeechRecognitionResult(transcript, 0.87F, words);
    }

    private String accessToken() {
        User user = userRepository.saveAndFlush(User.builder()
                .email(UUID.randomUUID() + "@contextstt.com")
                .password("encoded-password")
                .nickname("STT 테스터")
                .build());
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
    }
}
