package com.contextstt.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.domain.user.UserRepository;
import com.contextstt.backend.security.JwtTokenProvider;
import com.contextstt.backend.stt.RecognizedWord;
import com.contextstt.backend.stt.SpeechRecognitionResult;
import com.contextstt.backend.stt.SpeechToTextGateway;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConversationControllerTest {

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

    @BeforeEach
    void setUpGatewayMetadata() {
        when(speechToTextGateway.provider()).thenReturn("GOOGLE_SPEECH_V2");
        when(speechToTextGateway.model()).thenReturn("long");
    }

    @Test
    void createsListsAndGetsOwnedConversationWithParticipants() throws Exception {
        UserToken owner = userToken("대화 주인");

        MvcResult created = createConversation(owner.token(), "여행 일정 조율", "민수");
        Number conversationId = jsonNumber(created, "$.id");

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(1))
                .andExpect(jsonPath("$.conversations[0].id").value(conversationId.intValue()))
                .andExpect(jsonPath("$.conversations[0].title").value("여행 일정 조율"))
                .andExpect(jsonPath("$.conversations[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.conversations[0].utteranceCount").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId.longValue())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.participants[0].type").value("SELF"))
                .andExpect(jsonPath("$.participants[0].userId").value(owner.user().getId()))
                .andExpect(jsonPath("$.participants[0].displayName").value("대화 주인"))
                .andExpect(jsonPath("$.participants[1].type").value("OTHER"))
                .andExpect(jsonPath("$.participants[1].displayName").value("민수"))
                .andExpect(jsonPath("$.utterances.length()").value(0));

        mockMvc.perform(post("/api/conversations/{conversationId}/participants", conversationId.longValue())
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"영희\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("OTHER"))
                .andExpect(jsonPath("$.displayName").value("영희"));

        mockMvc.perform(post("/api/conversations/{conversationId}/participants", conversationId.longValue())
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"영희\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("같은 이름의 대화 참여자가 이미 존재합니다."));
    }

    @Test
    void attachesConfirmsAndLocksTranscriptionBeforeClosingConversation() throws Exception {
        UserToken owner = userToken("확정 테스터");
        MvcResult conversation = createConversation(owner.token(), "확정 흐름", null);
        Number conversationId = jsonNumber(conversation, "$.id");
        Number selfParticipantId = jsonNumber(conversation, "$.participants[0].id");

        MvcResult transcription = createTranscription(owner.token(), recognition("정보 손실", List.of(
                new RecognizedWord("정보", 0L, 200L, 0.92F, null),
                new RecognizedWord("손실", 210L, 500L, 0.75F, null)
        )));
        Number transcriptionId = jsonNumber(transcription, "$.id");
        Number wordId = jsonNumber(transcription, "$.words[1].id");

        MvcResult utterance = addUtterance(
                owner.token(),
                conversationId,
                transcriptionId,
                selfParticipantId
        );
        Number utteranceId = jsonNumber(utterance, "$.id");

        mockMvc.perform(post("/api/conversations/{conversationId}/utterances", conversationId.longValue())
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddUtterancePayload(
                                transcriptionId.longValue(),
                                selfParticipantId.longValue()
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 다른 대화 발언에 연결된 전사 기록입니다."));

        mockMvc.perform(post("/api/conversations/{conversationId}/close", conversationId.longValue())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("확정되지 않은 발언이 있어 대화를 종료할 수 없습니다."));

        mockMvc.perform(post(
                        "/api/conversations/{conversationId}/utterances/{utteranceId}/confirm",
                        conversationId.longValue(),
                        utteranceId.longValue()
                ).header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcription.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.transcription.confirmedAt").exists());

        mockMvc.perform(patch("/api/stt/transcriptions/{transcriptionId}/words/{wordId}",
                        transcriptionId.longValue(), wordId.longValue())
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"손씰\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("확정된 전사는 수정할 수 없습니다."));

        mockMvc.perform(multipart(
                        "/api/stt/transcriptions/{transcriptionId}/re-record",
                        transcriptionId.longValue()
                ).file(audio())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("확정된 전사는 다시 녹음할 수 없습니다."));

        mockMvc.perform(post("/api/conversations/{conversationId}/close", conversationId.longValue())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(post("/api/conversations/{conversationId}/participants", conversationId.longValue())
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"추가 참여자\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("종료된 대화는 변경할 수 없습니다."));
    }

    @Test
    void replacesDraftUtteranceOnlyWithItsRerecordedTranscription() throws Exception {
        UserToken owner = userToken("재발언 테스터");
        MvcResult conversation = createConversation(owner.token(), "재발언 흐름", null);
        Number conversationId = jsonNumber(conversation, "$.id");
        Number selfParticipantId = jsonNumber(conversation, "$.participants[0].id");

        MvcResult original = createTranscription(owner.token(), recognition("첫 발언", List.of()));
        Number originalId = jsonNumber(original, "$.id");
        MvcResult utterance = addUtterance(
                owner.token(),
                conversationId,
                originalId,
                selfParticipantId
        );
        Number utteranceId = jsonNumber(utterance, "$.id");

        MvcResult unrelated = createTranscription(owner.token(), recognition("별도 발언", List.of()));
        Number unrelatedId = jsonNumber(unrelated, "$.id");

        mockMvc.perform(patch(
                        "/api/conversations/{conversationId}/utterances/{utteranceId}/transcription",
                        conversationId.longValue(), utteranceId.longValue()
                ).header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transcriptionId\":" + unrelatedId.longValue() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("현재 발언을 대체하기 위해 생성된 전사가 아닙니다."));

        when(speechToTextGateway.recognize(any(byte[].class), eq("ko-KR")))
                .thenReturn(recognition("다시 한 발언", List.of()));
        MvcResult replacement = mockMvc.perform(multipart(
                        "/api/stt/transcriptions/{transcriptionId}/re-record",
                        originalId.longValue()
                ).file(audio())
                        .param("languageCode", "ko-KR")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isCreated())
                .andReturn();
        Number replacementId = jsonNumber(replacement, "$.id");

        mockMvc.perform(post("/api/conversations/{conversationId}/utterances", conversationId.longValue())
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddUtterancePayload(
                                replacementId.longValue(),
                                selfParticipantId.longValue()
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "재발언 전사는 새 발언으로 연결할 수 없습니다. 기존 발언 교체 API를 사용해 주세요."
                ));

        mockMvc.perform(patch(
                        "/api/conversations/{conversationId}/utterances/{utteranceId}/transcription",
                        conversationId.longValue(), utteranceId.longValue()
                ).header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transcriptionId\":" + replacementId.longValue() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcription.id").value(replacementId.intValue()))
                .andExpect(jsonPath("$.transcription.replacesTranscriptionId").value(originalId.intValue()))
                .andExpect(jsonPath("$.transcription.currentText").value("다시 한 발언"));

        mockMvc.perform(get("/api/stt/transcriptions/{transcriptionId}", originalId.longValue())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUPERSEDED"));

        mockMvc.perform(multipart(
                        "/api/stt/transcriptions/{transcriptionId}/re-record",
                        originalId.longValue()
                ).file(audio())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("재발언으로 대체된 전사는 다시 녹음할 수 없습니다."));

        mockMvc.perform(post("/api/conversations/{conversationId}/utterances", conversationId.longValue())
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddUtterancePayload(
                                originalId.longValue(),
                                selfParticipantId.longValue()
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("재발언으로 대체된 전사는 대화에 연결할 수 없습니다."));
    }

    @Test
    void hidesConversationsAndTranscriptionsOwnedByAnotherUser() throws Exception {
        UserToken owner = userToken("소유자");
        UserToken other = userToken("다른 사용자");
        MvcResult conversation = createConversation(owner.token(), "비공개 대화", null);
        Number conversationId = jsonNumber(conversation, "$.id");
        Number selfParticipantId = jsonNumber(conversation, "$.participants[0].id");

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId.longValue())
                        .header("Authorization", bearer(other.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("대화를 찾을 수 없습니다."));

        MvcResult othersTranscription = createTranscription(other.token(), recognition("다른 사람 발언", List.of()));
        Number othersTranscriptionId = jsonNumber(othersTranscription, "$.id");

        mockMvc.perform(post("/api/conversations/{conversationId}/utterances", conversationId.longValue())
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddUtterancePayload(
                                othersTranscriptionId.longValue(),
                                selfParticipantId.longValue()
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("전사 기록을 찾을 수 없습니다."));
    }

    @Test
    void validatesAuthenticationConversationInputAndPagination() throws Exception {
        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"대화\"}"))
                .andExpect(status().isUnauthorized());

        UserToken owner = userToken("검증 사용자");
        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"대화\",\"participants\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."));

        mockMvc.perform(get("/api/conversations")
                        .param("page", "-1")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("페이지 번호는 0 이상이어야 합니다."));

        mockMvc.perform(get("/api/conversations")
                        .param("size", "101")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상 100 이하여야 합니다."));

        mockMvc.perform(get("/api/conversations")
                        .param("page", "abc")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 파라미터 형식이 올바르지 않습니다."));

        mockMvc.perform(get("/api/conversations")
                        .param("size", "abc")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 파라미터 형식이 올바르지 않습니다."));
    }

    @Test
    void detailReturnsEachUtteranceOnceWithMultipleParticipants() throws Exception {
        UserToken owner = userToken("중복 검증 사용자");
        MvcResult conversation = createConversation(owner.token(), "중복 검증", "참여자 1");
        Number conversationId = jsonNumber(conversation, "$.id");
        Number selfParticipantId = jsonNumber(conversation, "$.participants[0].id");

        mockMvc.perform(post("/api/conversations/{conversationId}/participants", conversationId.longValue())
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"참여자 2\"}"))
                .andExpect(status().isCreated());

        MvcResult first = createTranscription(owner.token(), recognition("첫 번째 발언", List.of()));
        MvcResult second = createTranscription(owner.token(), recognition("두 번째 발언", List.of()));
        Number firstId = jsonNumber(first, "$.id");
        Number secondId = jsonNumber(second, "$.id");

        addUtterance(owner.token(), conversationId, firstId, selfParticipantId);
        MvcResult secondUtterance = mockMvc.perform(post(
                        "/api/conversations/{conversationId}/utterances",
                        conversationId.longValue()
                ).header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddUtterancePayload(
                                secondId.longValue(),
                                selfParticipantId.longValue()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order").value(1))
                .andReturn();
        Number secondUtteranceId = jsonNumber(secondUtterance, "$.id");

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId.longValue())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(3))
                .andExpect(jsonPath("$.utterances.length()").value(2))
                .andExpect(jsonPath("$.utterances[0].transcription.id").value(firstId.intValue()))
                .andExpect(jsonPath("$.utterances[1].id").value(secondUtteranceId.intValue()))
                .andExpect(jsonPath("$.utterances[1].transcription.id").value(secondId.intValue()));
    }

    private MvcResult createConversation(String token, String title, String otherParticipant) throws Exception {
        List<ParticipantPayload> participants = otherParticipant == null
                ? List.of()
                : List.of(new ParticipantPayload(otherParticipant));
        String payload = objectMapper.writeValueAsString(
                new ConversationPayload(title, "대화 상황", participants)
        );

        return mockMvc.perform(post("/api/conversations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult createTranscription(String token, SpeechRecognitionResult recognition) throws Exception {
        when(speechToTextGateway.recognize(any(byte[].class), eq("ko-KR"))).thenReturn(recognition);

        return mockMvc.perform(multipart("/api/stt/transcriptions")
                        .file(audio())
                        .param("languageCode", "ko-KR")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
    }

    private MvcResult addUtterance(
            String token,
            Number conversationId,
            Number transcriptionId,
            Number participantId
    ) throws Exception {
        String payload = objectMapper.writeValueAsString(new AddUtterancePayload(
                transcriptionId.longValue(),
                participantId.longValue()
        ));

        return mockMvc.perform(post("/api/conversations/{conversationId}/utterances", conversationId.longValue())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order").value(0))
                .andExpect(jsonPath("$.transcription.status").value("DRAFT"))
                .andReturn();
    }

    private SpeechRecognitionResult recognition(String transcript, List<RecognizedWord> words) {
        return new SpeechRecognitionResult(transcript, 0.9F, words);
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

    private record ParticipantPayload(String displayName) {
    }

    private record ConversationPayload(
            String title,
            String context,
            List<ParticipantPayload> participants
    ) {
    }

    private record AddUtterancePayload(Long transcriptionId, Long speakerParticipantId) {
    }
}
