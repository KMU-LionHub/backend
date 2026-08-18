package com.contextstt.backend.analysis.claude;

import com.contextstt.backend.analysis.ContextAnalysisGateway;
import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.exception.ContextAnalysisParseException;
import com.contextstt.backend.exception.ContextAnalysisProviderUnavailableException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ClaudeContextAnalysisGateway implements ContextAnalysisGateway {

    private static final String PROVIDER = "ANTHROPIC";

    private static final String SYSTEM_PROMPT = """
            당신은 대화 녹취록에서 맥락 손실 위험이 있는 표현을 찾아내는 어시스턴트입니다.
            대명사, 축약된 지칭, 배경 설명 없이 언급된 대상처럼 나중에 다시 보면 의미가 불분명해질 수 있는 부분을 찾고,
            그 부분이 가리킬 수 있는 맥락 후보를 신뢰도와 함께 제시하세요.
            반드시 아래 JSON 스키마와 정확히 일치하는 JSON만 출력하고, 다른 설명이나 코드블록은 추가하지 마세요.

            {"contextCandidates":[{"excerpt":"string","candidates":[{"content":"string","confidence":0.0}]}]}

            confidence는 0.0에서 1.0 사이의 값입니다. 맥락 손실 위험이 없으면 contextCandidates를 빈 배열로 반환하세요.
            """;

    private final RestClient claudeRestClient;
    private final ClaudeApiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public ContextAnalysisResult analyze(String transcriptText) {
        ensureConfigured();

        ClaudeMessageResponse response;
        try {
            response = claudeRestClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", properties.getApiKey())
                    .body(new ClaudeMessageRequest(
                            properties.getModel(),
                            properties.getMaxTokens(),
                            SYSTEM_PROMPT,
                            List.of(new ClaudeMessageRequest.ClaudeMessage("user", transcriptText))
                    ))
                    .retrieve()
                    .body(ClaudeMessageResponse.class);
        } catch (RestClientException ex) {
            throw new ContextAnalysisProviderUnavailableException("AI 분석 제공자 호출에 실패했습니다.", ex);
        }

        String text = extractText(response);
        return parseResult(text);
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public String model() {
        return properties.getModel();
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new ContextAnalysisProviderUnavailableException("AI 분석 기능이 활성화되어 있지 않습니다.");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new ContextAnalysisProviderUnavailableException("Claude API 키 설정이 필요합니다.");
        }
    }

    private String extractText(ClaudeMessageResponse response) {
        if (response == null || response.content() == null) {
            throw new ContextAnalysisProviderUnavailableException("AI 분석 응답이 비어 있습니다.");
        }
        return response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(ClaudeMessageResponse.ClaudeContentBlock::text)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseThrow(() -> new ContextAnalysisProviderUnavailableException("AI 분석 응답이 비어 있습니다."));
    }

    private ContextAnalysisResult parseResult(String text) {
        try {
            return objectMapper.readValue(stripCodeFence(text), ContextAnalysisResult.class);
        } catch (JacksonException ex) {
            throw new ContextAnalysisParseException("AI 응답을 JSON으로 해석할 수 없습니다.", ex);
        }
    }

    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            trimmed = trimmed.replaceFirst("```\\s*$", "");
        }
        return trimmed.trim();
    }
}
