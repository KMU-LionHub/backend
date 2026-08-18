package com.contextstt.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contextstt.backend.analysis.ContextAnalysisGateway;
import com.contextstt.backend.analysis.ContextAnalysisInput;
import com.contextstt.backend.analysis.ContextAnalysisModel;
import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.analysis.GeneratedContextAmbiguity;
import com.contextstt.backend.analysis.GeneratedContextCandidate;
import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.domain.user.UserRepository;
import com.contextstt.backend.exception.AnalysisProviderUnavailableException;
import com.contextstt.backend.security.JwtTokenProvider;
import com.contextstt.backend.stt.SpeechRecognitionResult;
import com.contextstt.backend.stt.SpeechToTextGateway;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
class ContextAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private SpeechToTextGateway speechToTextGateway;

    @MockitoBean
    private ContextAnalysisGateway analysisGateway;

    @BeforeEach
    void setUpSpeechGateway() {
        when(speechToTextGateway.provider()).thenReturn("GOOGLE_SPEECH_V2");
        when(speechToTextGateway.model()).thenReturn("long");
    }

    @Test
    void analyzesConfirmedUtteranceRanksCandidatesAndPersistsSpeakerSelection() throws Exception {
        UserToken owner = userToken("분석 사용자");
        ConversationSetup conversation = createConversation(owner.token());
        UtteranceSetup first = createUtterance(owner.token(), conversation, "이번 주말에 만날래?");
        confirm(owner.token(), conversation.id(), first.id());
        UtteranceSetup target = createUtterance(owner.token(), conversation, "일정을 좀 바야 할 것 같아");
        MvcResult targetTranscription = mockMvc.perform(get(
                        "/api/stt/transcriptions/{transcriptionId}",
                        target.transcriptionId()
                ).header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andReturn();
        Number correctedWordId = jsonNumber(targetTranscription, "$.words[2].id");
        mockMvc.perform(patch(
                        "/api/stt/transcriptions/{transcriptionId}/words/{wordId}",
                        target.transcriptionId(),
                        correctedWordId.longValue()
                ).header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"봐야\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentText").value("일정을 좀 봐야 할 것 같아"));
        confirm(owner.token(), conversation.id(), target.id());

        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        ))
                .thenReturn(validAnalysisResult());

        MvcResult created = analyze(owner.token(), conversation.id(), target.id(), 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("TEST_AI"))
                .andExpect(jsonPath("$.model").value("context-test-v1"))
                .andExpect(jsonPath("$.sourceOriginalText").value("일정을 좀 바야 할 것 같아"))
                .andExpect(jsonPath("$.sourceCurrentText").value("일정을 좀 봐야 할 것 같아"))
                .andExpect(jsonPath("$.requestedCandidateCount").value(3))
                .andExpect(jsonPath("$.ambiguityCount").value(1))
                .andExpect(jsonPath("$.needsClarification").value(true))
                .andExpect(jsonPath("$.ambiguities[0].excerpt").value("일정을"))
                .andExpect(jsonPath("$.ambiguities[0].startWordOrder").value(0))
                .andExpect(jsonPath("$.ambiguities[0].endWordOrder").value(0))
                .andExpect(jsonPath("$.ambiguities[0].candidates.length()").value(3))
                .andExpect(jsonPath("$.ambiguities[0].candidates[0].rank").value(1))
                .andExpect(jsonPath("$.ambiguities[0].candidates[0].interpretation")
                        .value("일정을 확인한 뒤 답하려는 의미"))
                .andExpect(jsonPath("$.ambiguities[0].candidates[0].intentSimilarityScore").value(0.91))
                .andExpect(jsonPath("$.ambiguities[0].candidates[1].intentSimilarityScore").value(0.72))
                .andExpect(jsonPath("$.ambiguities[0].candidates[2].intentSimilarityScore").value(0.55))
                .andExpect(jsonPath("$.ambiguities[0].selection").doesNotExist())
                .andReturn();

        Number analysisId = jsonNumber(created, "$.id");
        Number ambiguityId = jsonNumber(created, "$.ambiguities[0].id");
        Number firstCandidateId = jsonNumber(created, "$.ambiguities[0].candidates[0].id");

        ArgumentCaptor<ContextAnalysisInput> inputCaptor = ArgumentCaptor.forClass(ContextAnalysisInput.class);
        verify(analysisGateway).analyze(
                inputCaptor.capture(),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        );
        ContextAnalysisInput input = inputCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(input.conversationContext()).isEqualTo("친구와 약속을 정하는 대화");
        org.assertj.core.api.Assertions.assertThat(input.participants()).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(input.previousUtterances())
                .extracting(ContextAnalysisInput.PreviousUtterance::text)
                .containsExactly("이번 주말에 만날래?");
        org.assertj.core.api.Assertions.assertThat(input.omittedPreviousUtteranceCount()).isZero();
        org.assertj.core.api.Assertions.assertThat(input.targetSpeakerName()).isEqualTo("분석 사용자");
        org.assertj.core.api.Assertions.assertThat(input.targetOriginalText()).isEqualTo("일정을 좀 바야 할 것 같아");
        org.assertj.core.api.Assertions.assertThat(input.targetCurrentText()).isEqualTo("일정을 좀 봐야 할 것 같아");
        org.assertj.core.api.Assertions.assertThat(input.targetWords())
                .extracting(ContextAnalysisInput.AnalysisWord::text)
                .containsExactly("일정을", "좀", "봐야", "할", "것", "같아");

        mockMvc.perform(put(
                        "/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/selection",
                        analysisId.longValue(),
                        ambiguityId.longValue()
                )
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":" + firstCandidateId.longValue() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambiguities[0].selection.candidateId")
                        .value(firstCandidateId.intValue()))
                .andExpect(jsonPath("$.ambiguities[0].selection.finalText")
                        .value("일정을 확인한 뒤 답하려는 의미"))
                .andExpect(jsonPath("$.ambiguities[0].selection.edited").value(false))
                .andExpect(jsonPath("$.ambiguities[0].candidates[0].selected").value(true));

        mockMvc.perform(patch(
                        "/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/selection",
                        analysisId.longValue(),
                        ambiguityId.longValue()
                )
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"거절하려는 것이 아니라 일정을 확인한 뒤 답하려는 뜻\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambiguities[0].selection.finalText")
                        .value("거절하려는 것이 아니라 일정을 확인한 뒤 답하려는 뜻"))
                .andExpect(jsonPath("$.ambiguities[0].selection.edited").value(true));

        mockMvc.perform(get("/api/context-analyses/{analysisId}", analysisId.longValue())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambiguities[0].candidates.length()").value(3))
                .andExpect(jsonPath("$.ambiguities[0].selection.edited").value(true));

        mockMvc.perform(get("/api/context-analyses")
                        .param("conversationId", conversation.id().toString())
                        .param("utteranceId", target.id().toString())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analyses.length()").value(1))
                .andExpect(jsonPath("$.analyses[0].id").value(analysisId.intValue()))
                .andExpect(jsonPath("$.analyses[0].ambiguityCount").value(1))
                .andExpect(jsonPath("$.analyses[0].resolvedAmbiguityCount").value(1))
                .andExpect(jsonPath("$.analyses[0].fullyResolved").value(true));
    }

    @Test
    void analyzesDraftUtteranceSoItCanStillBeCorrected() throws Exception {
        UserToken owner = userToken("소유자");
        ConversationSetup conversation = createConversation(owner.token());
        UtteranceSetup draft = createUtterance(owner.token(), conversation, "아직 확정하지 않은 발언");

        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        ))
                .thenReturn(validAnalysisResult());

        MvcResult analysis = analyze(owner.token(), conversation.id(), draft.id(), 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceCurrentText").value("아직 확정하지 않은 발언"))
                .andExpect(jsonPath("$.stale").value(false))
                .andReturn();
        Number analysisId = jsonNumber(analysis, "$.id");
        Number ambiguityId = jsonNumber(analysis, "$.ambiguities[0].id");
        Number candidateId = jsonNumber(analysis, "$.ambiguities[0].candidates[0].id");

        MvcResult transcription = mockMvc.perform(get(
                        "/api/stt/transcriptions/{transcriptionId}",
                        draft.transcriptionId()
                ).header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andReturn();
        Number wordId = jsonNumber(transcription, "$.words[0].id");

        mockMvc.perform(patch(
                        "/api/stt/transcriptions/{transcriptionId}/words/{wordId}",
                        draft.transcriptionId(),
                        wordId.longValue()
                ).header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"아직은\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentText").value("아직은 확정하지 않은 발언"));

        mockMvc.perform(put(
                        "/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/selection",
                        analysisId.longValue(),
                        ambiguityId.longValue()
                ).header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":" + candidateId.longValue() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("분석 이후 전사가 변경되었습니다. 현재 발언으로 다시 분석해 주세요."));

        mockMvc.perform(get("/api/context-analyses/{analysisId}", analysisId.longValue())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stale").value(true))
                .andExpect(jsonPath("$.usableResolution").value(false));
    }

    @Test
    void returnsEmptyAmbiguitiesForClearUtterance() throws Exception {
        UserToken owner = userToken("명확한 발언 사용자");
        ConversationSetup conversation = createConversation(owner.token());
        UtteranceSetup utterance = createUtterance(owner.token(), conversation, "내일 오후 세 시에 만나자");
        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        )).thenReturn(new ContextAnalysisResult("TEST_AI", "context-test-v1", List.of()));

        analyze(owner.token(), conversation.id(), utterance.id(), 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.needsClarification").value(false))
                .andExpect(jsonPath("$.ambiguityCount").value(0))
                .andExpect(jsonPath("$.ambiguities").isEmpty());
    }

    @Test
    void resolvesAmbiguityWithCustomTextOrDismissal() throws Exception {
        UserToken owner = userToken("직접 확정 사용자");
        ConversationSetup conversation = createConversation(owner.token());
        UtteranceSetup utterance = createUtterance(owner.token(), conversation, "그거 내일까지 부탁해");
        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        )).thenReturn(validAnalysisResult());
        MvcResult created = analyze(owner.token(), conversation.id(), utterance.id(), 3)
                .andExpect(status().isCreated())
                .andReturn();
        Number analysisId = jsonNumber(created, "$.id");
        Number ambiguityId = jsonNumber(created, "$.ambiguities[0].id");

        mockMvc.perform(put(
                        "/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/resolution",
                        analysisId.longValue(),
                        ambiguityId.longValue()
                ).header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"CUSTOM","text":"주간 보고서를 내일까지 작성해 달라는 뜻"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambiguities[0].selection.type").value("CUSTOM"))
                .andExpect(jsonPath("$.ambiguities[0].selection.candidateId").doesNotExist())
                .andExpect(jsonPath("$.ambiguities[0].selection.finalText")
                        .value("주간 보고서를 내일까지 작성해 달라는 뜻"))
                .andExpect(jsonPath("$.fullyResolved").value(true))
                .andExpect(jsonPath("$.usableResolution").value(true));

        mockMvc.perform(put(
                        "/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/resolution",
                        analysisId.longValue(),
                        ambiguityId.longValue()
                ).header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DISMISSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambiguities[0].selection.type").value("DISMISSED"))
                .andExpect(jsonPath("$.ambiguities[0].selection.finalText").doesNotExist())
                .andExpect(jsonPath("$.fullyResolved").value(true));

        mockMvc.perform(put(
                        "/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/resolution",
                        analysisId.longValue(),
                        ambiguityId.longValue()
                ).header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CUSTOM\",\"candidateId\":1,\"text\":\"잘못된 조합\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void routesExplicitOpenRouterModelSelection() throws Exception {
        UserToken owner = userToken("모델 선택 사용자");
        ConversationSetup conversation = createConversation(owner.token());
        UtteranceSetup draft = createUtterance(owner.token(), conversation, "빠른 모델로 분석할 발언");
        ContextAnalysisResult openRouterResult = new ContextAnalysisResult(
                "OPENROUTER",
                "google/gemini-3.7-flash",
                validAnalysisResult().ambiguities()
        );

        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.GEMINI_3_7_FLASH)
        )).thenReturn(openRouterResult);

        analyze(
                owner.token(),
                conversation.id(),
                draft.id(),
                3,
                ContextAnalysisModel.GEMINI_3_7_FLASH
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("OPENROUTER"))
                .andExpect(jsonPath("$.model").value("google/gemini-3.7-flash"));

        verify(analysisGateway).analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.GEMINI_3_7_FLASH)
        );
    }

    @Test
    void rejectsUnknownModelValueBeforeCallingProvider() throws Exception {
        UserToken owner = userToken("잘못된 모델 사용자");
        String payload = """
                {
                  "conversationId": 1,
                  "utteranceId": 1,
                  "candidateCount": 3,
                  "model": "ARBITRARY_EXPENSIVE_MODEL"
                }
                """;

        mockMvc.perform(post("/api/context-analyses")
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(analysisGateway);
    }

    @Test
    void rejectsAnotherUsersConversationBeforeCallingProvider() throws Exception {
        UserToken owner = userToken("소유자");
        UserToken other = userToken("다른 사용자");
        ConversationSetup conversation = createConversation(owner.token());
        UtteranceSetup draft = createUtterance(owner.token(), conversation, "소유자의 발언");

        analyze(other.token(), conversation.id(), draft.id(), 3)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("대화를 찾을 수 없습니다."));

        verifyNoInteractions(analysisGateway);
    }

    @Test
    void mapsUnavailableAndInvalidProviderResultsWithoutSavingAnalysis() throws Exception {
        UserToken owner = userToken("제공자 검증 사용자");
        ConversationSetup conversation = createConversation(owner.token());
        UtteranceSetup target = createUtterance(owner.token(), conversation, "분석할 발언");
        confirm(owner.token(), conversation.id(), target.id());

        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        ))
                .thenThrow(new AnalysisProviderUnavailableException("분석 제공자 장애"));
        analyze(owner.token(), conversation.id(), target.id(), 3)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("분석 제공자 장애"));

        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        ))
                .thenReturn(new ContextAnalysisResult("TEST_AI", "broken-model", null));
        analyze(owner.token(), conversation.id(), target.id(), 3)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("맥락 분석 제공자가 올바르지 않은 결과를 반환했습니다."));

        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        ))
                .thenReturn(new ContextAnalysisResult(
                        "TEST_AI",
                        "broken-model",
                        List.of(new GeneratedContextAmbiguity(
                                0,
                                0,
                                List.of(candidate("후보 하나", "의도", "근거", "0.5"))
                        ))
                ));
        analyze(owner.token(), conversation.id(), target.id(), 3)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("맥락 분석 제공자가 올바르지 않은 결과를 반환했습니다."));

        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        )).thenReturn(new ContextAnalysisResult(
                "TEST_AI",
                "broken-model",
                List.of(new GeneratedContextAmbiguity(
                        999,
                        999,
                        validAnalysisResult().ambiguities().getFirst().candidates()
                ))
        ));
        analyze(owner.token(), conversation.id(), target.id(), 3)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("맥락 분석 제공자가 올바르지 않은 결과를 반환했습니다."));

        mockMvc.perform(get("/api/context-analyses")
                        .param("conversationId", conversation.id().toString())
                        .param("utteranceId", target.id().toString())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analyses.length()").value(0));
    }

    @Test
    void validatesCandidateCountAndRequiresSelectionBeforeEditing() throws Exception {
        UserToken owner = userToken("선택 검증 사용자");
        ConversationSetup conversation = createConversation(owner.token());
        UtteranceSetup target = createUtterance(owner.token(), conversation, "선택 검증 발언");
        confirm(owner.token(), conversation.id(), target.id());

        analyze(owner.token(), conversation.id(), target.id(), 1)
                .andExpect(status().isBadRequest());

        when(analysisGateway.analyze(
                any(ContextAnalysisInput.class),
                eq(3),
                eq(ContextAnalysisModel.CLAUDE_SONNET_5)
        ))
                .thenReturn(validAnalysisResult());
        MvcResult created = analyze(owner.token(), conversation.id(), target.id(), null)
                .andExpect(status().isCreated())
                .andReturn();
        Number analysisId = jsonNumber(created, "$.id");
        Number ambiguityId = jsonNumber(created, "$.ambiguities[0].id");

        mockMvc.perform(patch(
                        "/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/selection",
                        analysisId.longValue(),
                        ambiguityId.longValue()
                )
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"직접 수정\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("먼저 맥락 후보를 선택해 주세요."));

        mockMvc.perform(put(
                        "/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/selection",
                        analysisId.longValue(),
                        ambiguityId.longValue()
                )
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("맥락 후보를 찾을 수 없습니다."));
    }

    private org.springframework.test.web.servlet.ResultActions analyze(
            String token,
            Long conversationId,
            Long utteranceId,
            Integer candidateCount
    ) throws Exception {
        return analyze(token, conversationId, utteranceId, candidateCount, null);
    }

    private org.springframework.test.web.servlet.ResultActions analyze(
            String token,
            Long conversationId,
            Long utteranceId,
            Integer candidateCount,
            ContextAnalysisModel model
    ) throws Exception {
        String payload = objectMapper.writeValueAsString(
                new AnalysisPayload(conversationId, utteranceId, candidateCount, model)
        );
        return mockMvc.perform(post("/api/context-analyses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));
    }

    private ConversationSetup createConversation(String token) throws Exception {
        String payload = """
                {
                  "title": "약속 대화",
                  "context": "친구와 약속을 정하는 대화",
                  "participants": [{"displayName": "친구"}]
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return new ConversationSetup(
                jsonNumber(result, "$.id").longValue(),
                jsonNumber(result, "$.participants[0].id").longValue()
        );
    }

    private UtteranceSetup createUtterance(
            String token,
            ConversationSetup conversation,
            String transcript
    ) throws Exception {
        when(speechToTextGateway.recognize(any(byte[].class), eq("ko-KR")))
                .thenReturn(new SpeechRecognitionResult(transcript, 0.9F, List.of()));
        MvcResult transcription = mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(audio())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        Long transcriptionId = jsonNumber(transcription, "$.id").longValue();

        String payload = objectMapper.writeValueAsString(
                new UtterancePayload(transcriptionId, conversation.selfParticipantId())
        );
        MvcResult utterance = mockMvc.perform(post(
                        "/api/conversations/{conversationId}/utterances",
                        conversation.id()
                ).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return new UtteranceSetup(
                jsonNumber(utterance, "$.id").longValue(),
                transcriptionId
        );
    }

    private void confirm(String token, Long conversationId, Long utteranceId) throws Exception {
        mockMvc.perform(post(
                        "/api/conversations/{conversationId}/utterances/{utteranceId}/confirm",
                        conversationId,
                        utteranceId
                ).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private ContextAnalysisResult validAnalysisResult() {
        return new ContextAnalysisResult(
                "TEST_AI",
                "context-test-v1",
                List.of(
                        new GeneratedContextAmbiguity(
                                0,
                                0,
                                List.of(
                                        candidate("완곡하게 거절하려는 의미", "만남 거절", "답변을 미루는 표현", "0.55"),
                                        candidate("일정을 확인한 뒤 답하려는 의미", "일정 확인", "명시적으로 일정을 보겠다고 말함", "0.91"),
                                        candidate("다른 날짜를 제안하려는 의미", "일정 변경", "현재 일정이 어려울 가능성", "0.72")
                                )
                        )
                )
        );
    }

    private GeneratedContextCandidate candidate(
            String interpretation,
            String intent,
            String rationale,
            String score
    ) {
        return new GeneratedContextCandidate(
                interpretation,
                intent,
                rationale,
                new BigDecimal(score)
        );
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[]{1, 2, 3});
    }

    private Number jsonNumber(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private UserToken userToken(String nickname) {
        User user = userRepository.saveAndFlush(User.builder()
                .email(UUID.randomUUID() + "@contextstt.com")
                .password("encoded-password")
                .nickname(nickname)
                .build());
        return new UserToken(user, jwtTokenProvider.createAccessToken(user.getId(), user.getEmail()));
    }

    private record UserToken(User user, String token) {
    }

    private record ConversationSetup(Long id, Long selfParticipantId) {
    }

    private record UtteranceSetup(Long id, Long transcriptionId) {
    }

    private record AnalysisPayload(
            Long conversationId,
            Long utteranceId,
            Integer candidateCount,
            ContextAnalysisModel model
    ) {
    }

    private record UtterancePayload(Long transcriptionId, Long speakerParticipantId) {
    }
}
