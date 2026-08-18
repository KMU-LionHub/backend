package com.contextstt.backend.analysis.openrouter;

import com.contextstt.backend.analysis.ContextAnalysisInput;
import com.contextstt.backend.analysis.ContextAnalysisModel;
import com.contextstt.backend.analysis.ContextAnalysisProvider;
import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.analysis.prompt.ContextAnalysisOutputParser;
import com.contextstt.backend.analysis.prompt.ContextAnalysisPrompt;
import com.contextstt.backend.analysis.prompt.ContextAnalysisPromptFactory;
import com.contextstt.backend.exception.AnalysisProviderUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenRouterContextAnalysisProvider implements ContextAnalysisProvider {

    private static final String PROVIDER = "OPENROUTER";
    private static final String HTTP_REFERER = "HTTP-Referer";
    private static final String OPENROUTER_TITLE = "X-OpenRouter-Title";

    private final RestClient openRouterRestClient;
    private final OpenRouterApiProperties properties;
    private final ContextAnalysisPromptFactory promptFactory;
    private final ContextAnalysisOutputParser outputParser;

    public OpenRouterContextAnalysisProvider(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            OpenRouterApiProperties properties,
            ContextAnalysisPromptFactory promptFactory,
            ContextAnalysisOutputParser outputParser
    ) {
        this.openRouterRestClient = openRouterRestClient;
        this.properties = properties;
        this.promptFactory = promptFactory;
        this.outputParser = outputParser;
    }

    @Override
    public boolean supports(ContextAnalysisModel model) {
        return model == ContextAnalysisModel.GEMINI_3_7_FLASH
                || model == ContextAnalysisModel.DEEPSEEK_V4_FLASH;
    }

    @Override
    public ContextAnalysisResult analyze(
            ContextAnalysisInput input,
            int candidateCount,
            ContextAnalysisModel model
    ) {
        ensureConfigured();
        String modelId = properties.modelId(model);
        ContextAnalysisPrompt prompt = promptFactory.create(input, candidateCount);
        OpenRouterChatRequest body = OpenRouterChatRequest.contextAnalysis(
                modelId,
                prompt.systemMessage(),
                prompt.userMessage(),
                properties.getMaxTokens(),
                candidateCount
        );

        OpenRouterChatResponse response;
        try {
            RestClient.RequestBodySpec request = openRouterRestClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .header(OPENROUTER_TITLE, properties.getAppTitle())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(properties.getSiteUrl())) {
                request.header(HTTP_REFERER, properties.getSiteUrl());
            }
            response = request.body(body)
                    .retrieve()
                    .body(OpenRouterChatResponse.class);
        } catch (RestClientException ex) {
            throw new AnalysisProviderUnavailableException("OpenRouter 분석 제공자 호출에 실패했습니다.", ex);
        }

        return outputParser.parse(extractText(response), PROVIDER, modelId);
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new AnalysisProviderUnavailableException("OpenRouter 분석 기능이 활성화되어 있지 않습니다.");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new AnalysisProviderUnavailableException("OpenRouter API 키 설정이 필요합니다.");
        }
    }

    private String extractText(OpenRouterChatResponse response) {
        if (response == null || response.choices() == null) {
            return null;
        }
        return response.choices().stream()
                .map(OpenRouterChatResponse.OpenRouterChoice::message)
                .filter(message -> message != null && StringUtils.hasText(message.content()))
                .map(OpenRouterChatResponse.OpenRouterMessage::content)
                .findFirst()
                .orElse(null);
    }
}
