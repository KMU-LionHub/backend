package com.contextstt.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contextstt.backend.analysis.ContextAnalysisGateway;
import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.analysis.ContextCandidateGroup;
import com.contextstt.backend.analysis.ContextCandidateOption;
import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.domain.user.UserRepository;
import com.contextstt.backend.exception.ContextAnalysisProviderUnavailableException;
import com.contextstt.backend.security.JwtTokenProvider;
import com.contextstt.backend.stt.RecognizedWord;
import com.contextstt.backend.stt.SpeechRecognitionResult;
import com.contextstt.backend.stt.SpeechToTextGateway;
import java.time.Duration;
import java.time.Instant;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SpeechToTextGateway speechToTextGateway;

    @MockitoBean
    private ContextAnalysisGateway contextAnalysisGateway;

    @BeforeEach
    void setUpGatewayMetadata() {
        when(speechToTextGateway.provider()).thenReturn("GOOGLE_SPEECH_V2");
        when(speechToTextGateway.model()).thenReturn("long");
        when(contextAnalysisGateway.provider()).thenReturn("ANTHROPIC");
        when(contextAnalysisGateway.model()).thenReturn("claude-sonnet-5");
    }

    @Test
    void submitAnalysisAndPollUntilCompleted() throws Exception {
        String token = accessToken();
        Long transcriptionId = createTranscription(token, "그거 이번 주까지 하기로 한 거 맞죠?");

        ContextAnalysisResult result = new ContextAnalysisResult(List.of(
                new ContextCandidateGroup(
                        "그거 이번 주까지 하기로 한 거 맞죠?",
                        List.of(
                                new ContextCandidateOption("이전 발언에서 언급된 작업을 지칭", 0.7),
                                new ContextCandidateOption("직전 회의에서 결정된 일정 항목을 지칭", 0.3)
                        )
                )
        ));
        when(contextAnalysisGateway.analyze(anyString())).thenReturn(result);

        String submitPayload = objectMapper.writeValueAsString(new AnalysisSubmitPayload(transcriptionId));
        MvcResult submitResult = mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitPayload)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.analysisId").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Long analysisId = objectMapper.readTree(submitResult.getResponse().getContentAsString())
                .get("analysisId").asLong();

        String status = pollUntilFinished(token, analysisId);

        assertThat(status).isEqualTo("COMPLETED");

        mockMvc.perform(get("/api/analysis/" + analysisId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value(100))
                .andExpect(jsonPath("$.contextCandidates.length()").value(1))
                .andExpect(jsonPath("$.contextCandidates[0].excerpt").value("그거 이번 주까지 하기로 한 거 맞죠?"))
                .andExpect(jsonPath("$.contextCandidates[0].candidates.length()").value(2))
                .andExpect(jsonPath("$.contextCandidates[0].candidates[0].confidence").value(0.7));
    }

    @Test
    void analysisFailsWhenProviderUnavailable() throws Exception {
        String token = accessToken();
        Long transcriptionId = createTranscription(token, "발화 내용");

        when(contextAnalysisGateway.analyze(anyString()))
                .thenThrow(new ContextAnalysisProviderUnavailableException("AI 분석 기능이 활성화되어 있지 않습니다."));

        String submitPayload = objectMapper.writeValueAsString(new AnalysisSubmitPayload(transcriptionId));
        MvcResult submitResult = mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitPayload)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn();

        Long analysisId = objectMapper.readTree(submitResult.getResponse().getContentAsString())
                .get("analysisId").asLong();

        String status = pollUntilFinished(token, analysisId);

        assertThat(status).isEqualTo("FAILED");

        mockMvc.perform(get("/api/analysis/" + analysisId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("AI 분석 기능이 활성화되어 있지 않습니다."));
    }

    @Test
    void submitAnalysisForUnknownTranscriptionReturnsNotFound() throws Exception {
        String token = accessToken();

        String submitPayload = objectMapper.writeValueAsString(new AnalysisSubmitPayload(999999L));
        mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitPayload)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void analysisWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/analysis/1"))
                .andExpect(status().isUnauthorized());
    }

    private Long createTranscription(String token, String transcript) throws Exception {
        when(speechToTextGateway.recognize(any(byte[].class), eq("ko-KR")))
                .thenReturn(new SpeechRecognitionResult(transcript, 0.9F, List.of(
                        new RecognizedWord(transcript, 0L, 500L, 0.9F, null)
                )));

        MvcResult result = mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[]{1, 2, 3, 4}))
                        .param("languageCode", "ko-KR")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String pollUntilFinished(String token, Long analysisId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            MvcResult result = mockMvc.perform(get("/api/analysis/" + analysisId)
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
            String currentStatus = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("status").asText();
            if (!"PENDING".equals(currentStatus) && !"IN_PROGRESS".equals(currentStatus)) {
                return currentStatus;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("분석이 제한 시간 내에 끝나지 않았습니다.");
    }

    private String accessToken() {
        User user = userRepository.saveAndFlush(User.builder()
                .email(UUID.randomUUID() + "@contextstt.com")
                .password("encoded-password")
                .nickname("분석 테스터")
                .build());
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
    }

    private record AnalysisSubmitPayload(Long transcriptionId) {
    }
}
