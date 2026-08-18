package com.contextstt.backend.analysis.openrouter;

import com.contextstt.backend.analysis.prompt.ContextAnalysisJsonSchema;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

record OpenRouterChatRequest(
        String model,
        List<OpenRouterMessage> messages,
        @JsonProperty("max_tokens") int maxTokens,
        @JsonProperty("response_format") OpenRouterResponseFormat responseFormat,
        OpenRouterProviderPreferences provider
) {

    static OpenRouterChatRequest contextAnalysis(
            String model,
            String systemMessage,
            String userMessage,
            int maxTokens,
            int candidateCount
    ) {
        return new OpenRouterChatRequest(
                model,
                List.of(
                        new OpenRouterMessage("system", systemMessage),
                        new OpenRouterMessage("user", userMessage)
                ),
                maxTokens,
                new OpenRouterResponseFormat(
                        "json_schema",
                        new OpenRouterJsonSchema(
                                "context_analysis",
                                true,
                                ContextAnalysisJsonSchema.forCandidateCount(candidateCount)
                        )
                ),
                new OpenRouterProviderPreferences(true)
        );
    }

    record OpenRouterMessage(String role, String content) {
    }

    record OpenRouterResponseFormat(
            String type,
            @JsonProperty("json_schema") OpenRouterJsonSchema jsonSchema
    ) {
    }

    record OpenRouterJsonSchema(String name, boolean strict, Map<String, Object> schema) {
    }

    record OpenRouterProviderPreferences(
            @JsonProperty("require_parameters") boolean requireParameters
    ) {
    }
}
