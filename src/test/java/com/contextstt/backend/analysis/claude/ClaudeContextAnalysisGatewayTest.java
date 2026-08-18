package com.contextstt.backend.analysis.claude;

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

class ClaudeContextAnalysisGatewayTest {

    private ClaudeApiProperties properties;
    private MockRestServiceServer server;
    private ClaudeContextAnalysisProvider provider;

    @BeforeEach
    void setUp() {
        properties = new ClaudeApiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-api-key");
        properties.setBaseUrl("https://claude.test");
        properties.setModel("claude-test-model");

        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("anthropic-version", "2023-06-01");
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        ObjectMapper objectMapper = new ObjectMapper();
        provider = new ClaudeContextAnalysisProvider(
                restClientBuilder.build(),
                properties,
                new ContextAnalysisPromptFactory(objectMapper),
                new ContextAnalysisOutputParser(objectMapper)
        );
    }

    @Test
    void sendsWholeConversationInputAndMapsCandidates() {
        server.expect(requestTo("https://claude.test/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(content().string(containsString("친구와 약속을 정하는 대화")))
                .andExpect(content().string(containsString("이번 주말에 만날래?")))
                .andExpect(content().string(containsString("생성할 후보 수: 2")))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        ContextAnalysisResult result = provider.analyze(
                input(),
                2,
                ContextAnalysisModel.CLAUDE_SONNET_5
        );

        assertThat(result.provider()).isEqualTo("ANTHROPIC");
        assertThat(result.model()).isEqualTo("claude-test-model");
        assertThat(result.ambiguities()).hasSize(1);
        assertThat(result.ambiguities().getFirst().startWordOrder()).isZero();
        assertThat(result.ambiguities().getFirst().candidates()).hasSize(2);
        assertThat(result.ambiguities().getFirst().candidates().getFirst().interpretation())
                .isEqualTo("일정을 확인한 뒤 답하려는 의미");
        assertThat(result.ambiguities().getFirst().candidates().getFirst().intentSimilarityScore())
                .isEqualByComparingTo(new BigDecimal("0.91"));
        server.verify();
    }

    @Test
    void rejectsCallsWhenClaudeIsDisabledOrApiKeyIsMissing() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> provider.analyze(input(), 2, ContextAnalysisModel.CLAUDE_SONNET_5))
                .isInstanceOf(AnalysisProviderUnavailableException.class)
                .hasMessage("AI 분석 기능이 활성화되어 있지 않습니다.");

        properties.setEnabled(true);
        properties.setApiKey("  ");

        assertThatThrownBy(() -> provider.analyze(input(), 2, ContextAnalysisModel.CLAUDE_SONNET_5))
                .isInstanceOf(AnalysisProviderUnavailableException.class)
                .hasMessage("Claude API 키 설정이 필요합니다.");
    }

    @Test
    void mapsProviderFailureToServiceUnavailableError() {
        server.expect(requestTo("https://claude.test/v1/messages"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> provider.analyze(input(), 2, ContextAnalysisModel.CLAUDE_SONNET_5))
                .isInstanceOf(AnalysisProviderUnavailableException.class)
                .hasMessage("AI 분석 제공자 호출에 실패했습니다.");
        server.verify();
    }

    @Test
    void rejectsMalformedProviderResponse() {
        String response = """
                {
                  "id": "msg_test",
                  "content": [{"type": "text", "text": "not-json"}]
                }
                """;
        server.expect(requestTo("https://claude.test/v1/messages"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.analyze(input(), 2, ContextAnalysisModel.CLAUDE_SONNET_5))
                .isInstanceOf(InvalidAnalysisResultException.class)
                .hasMessage("AI 분석 응답을 해석할 수 없습니다.");
        server.verify();
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

    private String successResponse() {
        String payload = """
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
        return new ObjectMapper().writeValueAsString(new ClaudeMessageResponse(
                "msg_test",
                List.of(new ClaudeMessageResponse.ClaudeContentBlock("text", payload))
        ));
    }
}
