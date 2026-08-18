package com.contextstt.backend.analysis.openrouter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.contextstt.backend.analysis.ContextAnalysisInput;
import com.contextstt.backend.analysis.ContextAnalysisInput.AnalysisParticipant;
import com.contextstt.backend.analysis.ContextAnalysisInput.PreviousUtterance;
import com.contextstt.backend.analysis.ContextAnalysisModel;
import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.analysis.prompt.ContextAnalysisOutputParser;
import com.contextstt.backend.analysis.prompt.ContextAnalysisPromptFactory;
import com.contextstt.backend.exception.AnalysisProviderUnavailableException;
import com.contextstt.backend.exception.InvalidAnalysisResultException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class OpenRouterContextAnalysisProviderTest {

    private OpenRouterApiProperties properties;
    private MockRestServiceServer server;
    private OpenRouterContextAnalysisProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new OpenRouterApiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-openrouter-key");
        properties.setBaseUrl("https://openrouter.test/api/v1");
        properties.setSiteUrl("https://context-stt.test");
        properties.setAppTitle("Context STT Test");

        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(properties.getBaseUrl());
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        objectMapper = new ObjectMapper();
        provider = new OpenRouterContextAnalysisProvider(
                restClientBuilder.build(),
                properties,
                new ContextAnalysisPromptFactory(objectMapper),
                new ContextAnalysisOutputParser(objectMapper)
        );
    }

    @Test
    void analyzesWithGeminiUsingStrictStructuredOutput() {
        expectSuccessfulRequest("google/gemini-3.7-flash");

        ContextAnalysisResult result = provider.analyze(
                input(),
                2,
                ContextAnalysisModel.GEMINI_3_7_FLASH
        );

        assertThat(result.provider()).isEqualTo("OPENROUTER");
        assertThat(result.model()).isEqualTo("google/gemini-3.7-flash");
        assertThat(result.ambiguities()).hasSize(1);
        assertThat(result.ambiguities().getFirst().candidates()).hasSize(2);
        assertThat(result.ambiguities().getFirst().candidates().getFirst().intentSimilarityScore())
                .isEqualByComparingTo(new BigDecimal("0.91"));
        server.verify();
    }

    @Test
    void analyzesWithDeepSeekV4FlashSlug() {
        expectSuccessfulRequest("deepseek/deepseek-v4-flash");

        ContextAnalysisResult result = provider.analyze(
                input(),
                2,
                ContextAnalysisModel.DEEPSEEK_V4_FLASH
        );

        assertThat(result.model()).isEqualTo("deepseek/deepseek-v4-flash");
        assertThat(result.ambiguities()).hasSize(1);
        server.verify();
    }

    @Test
    void rejectsCallsWhenOpenRouterIsDisabledOrApiKeyIsMissing() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> provider.analyze(
                input(),
                2,
                ContextAnalysisModel.GEMINI_3_7_FLASH
        ))
                .isInstanceOf(AnalysisProviderUnavailableException.class)
                .hasMessage("OpenRouter 분석 기능이 활성화되어 있지 않습니다.");

        properties.setEnabled(true);
        properties.setApiKey("  ");

        assertThatThrownBy(() -> provider.analyze(
                input(),
                2,
                ContextAnalysisModel.DEEPSEEK_V4_FLASH
        ))
                .isInstanceOf(AnalysisProviderUnavailableException.class)
                .hasMessage("OpenRouter API 키 설정이 필요합니다.");
    }

    @Test
    void mapsOpenRouterFailureToServiceUnavailableError() {
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.analyze(
                input(),
                2,
                ContextAnalysisModel.GEMINI_3_7_FLASH
        ))
                .isInstanceOf(AnalysisProviderUnavailableException.class)
                .hasMessage("OpenRouter 분석 제공자 호출에 실패했습니다.");
        server.verify();
    }

    @Test
    void rejectsResponseWithoutAssistantContent() {
        String response = objectMapper.writeValueAsString(
                new OpenRouterChatResponse("chat_test", "google/gemini-3.7-flash", List.of())
        );
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.analyze(
                input(),
                2,
                ContextAnalysisModel.GEMINI_3_7_FLASH
        ))
                .isInstanceOf(InvalidAnalysisResultException.class)
                .hasMessage("AI 분석 응답이 비어 있습니다.");
        server.verify();
    }

    private void expectSuccessfulRequest(String model) {
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-openrouter-key"))
                .andExpect(header("HTTP-Referer", "https://context-stt.test"))
                .andExpect(header("X-OpenRouter-Title", "Context STT Test"))
                .andExpect(content().string(containsString("\"model\":\"" + model + "\"")))
                .andExpect(content().string(containsString("\"type\":\"json_schema\"")))
                .andExpect(content().string(containsString("\"strict\":true")))
                .andExpect(content().string(containsString("\"require_parameters\":true")))
                .andExpect(content().string(containsString("\"minItems\":2")))
                .andExpect(content().string(containsString("\"maxItems\":2")))
                .andExpect(content().string(containsString("친구와 약속을 정하는 대화")))
                .andRespond(withSuccess(successResponse(model), MediaType.APPLICATION_JSON));
    }

    private ContextAnalysisInput input() {
        return new ContextAnalysisInput(
                5L,
                12L,
                "친구와 약속을 정하는 대화",
                List.of(
                        new AnalysisParticipant(1L, "화자", true),
                        new AnalysisParticipant(2L, "친구", false)
                ),
                List.of(new PreviousUtterance(0, "친구", "이번 주말에 만날래?")),
                "화자",
                "일정을 좀 바야 할 것 같아",
                "일정을 좀 봐야 할 것 같아",
                List.of(
                        new ContextAnalysisInput.AnalysisWord(0, "일정을"),
                        new ContextAnalysisInput.AnalysisWord(1, "좀"),
                        new ContextAnalysisInput.AnalysisWord(2, "봐야"),
                        new ContextAnalysisInput.AnalysisWord(3, "할"),
                        new ContextAnalysisInput.AnalysisWord(4, "것"),
                        new ContextAnalysisInput.AnalysisWord(5, "같아")
                )
        );
    }

    private String successResponse(String model) {
        OpenRouterChatResponse.OpenRouterMessage message = new OpenRouterChatResponse.OpenRouterMessage(
                "assistant",
                candidatePayload()
        );
        return objectMapper.writeValueAsString(new OpenRouterChatResponse(
                "chat_test",
                model,
                List.of(new OpenRouterChatResponse.OpenRouterChoice(0, message))
        ));
    }

    private String candidatePayload() {
        return """
                {"ambiguities":[
                  {
                    "startWordOrder":0,
                    "endWordOrder":5,
                    "candidates":[
                      {
                        "interpretation":"일정을 확인한 뒤 답하려는 의미",
                        "inferredIntent":"일정 확인",
                        "rationale":"일정을 보겠다고 말함",
                        "intentSimilarityScore":0.91
                      },
                      {
                        "interpretation":"다른 날짜를 제안하려는 의미",
                        "inferredIntent":"일정 변경",
                        "rationale":"현재 일정이 어려울 수 있음",
                        "intentSimilarityScore":0.72
                      }
                    ]
                  }
                ]}
                """;
    }
}
