package com.contextstt.backend.analysis.claude;

import com.contextstt.backend.analysis.ContextAnalysisInput;
import com.contextstt.backend.analysis.ContextAnalysisModel;
import com.contextstt.backend.analysis.ContextAnalysisProvider;
import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.analysis.prompt.ContextAnalysisOutputParser;
import com.contextstt.backend.analysis.prompt.ContextAnalysisPrompt;
import com.contextstt.backend.analysis.prompt.ContextAnalysisPromptFactory;
import com.contextstt.backend.exception.AnalysisProviderUnavailableException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ClaudeContextAnalysisProvider implements ContextAnalysisProvider {

    private static final String PROVIDER = "ANTHROPIC";

    private final RestClient claudeRestClient;
    private final ClaudeApiProperties properties;
    private final ContextAnalysisPromptFactory promptFactory;
    private final ContextAnalysisOutputParser outputParser;

    public ClaudeContextAnalysisProvider(
            @Qualifier("claudeRestClient") RestClient claudeRestClient,
            ClaudeApiProperties properties,
            ContextAnalysisPromptFactory promptFactory,
            ContextAnalysisOutputParser outputParser
    ) {
        this.claudeRestClient = claudeRestClient;
        this.properties = properties;
        this.promptFactory = promptFactory;
        this.outputParser = outputParser;
    }

    @Override
    public boolean supports(ContextAnalysisModel model) {
        return model == ContextAnalysisModel.CLAUDE_SONNET_5;
    }

    @Override
    public ContextAnalysisResult analyze(
            ContextAnalysisInput input,
            int candidateCount,
            ContextAnalysisModel model
    ) {
        ensureConfigured();
        ContextAnalysisPrompt prompt = promptFactory.create(input, candidateCount);

        ClaudeMessageResponse response;
        try {
            response = claudeRestClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new ClaudeMessageRequest(
                            properties.getModel(),
                            properties.getMaxTokens(),
                            prompt.systemMessage(),
                            List.of(new ClaudeMessageRequest.ClaudeMessage("user", prompt.userMessage()))
                    ))
                    .retrieve()
                    .body(ClaudeMessageResponse.class);
        } catch (RestClientException ex) {
            throw new AnalysisProviderUnavailableException("AI 분석 제공자 호출에 실패했습니다.", ex);
        }

        return outputParser.parse(extractText(response), PROVIDER, properties.getModel());
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new AnalysisProviderUnavailableException("AI 분석 기능이 활성화되어 있지 않습니다.");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new AnalysisProviderUnavailableException("Claude API 키 설정이 필요합니다.");
        }
    }

    private String extractText(ClaudeMessageResponse response) {
        if (response == null || response.content() == null) {
            return null;
        }
        return response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(ClaudeMessageResponse.ClaudeContentBlock::text)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }
}
