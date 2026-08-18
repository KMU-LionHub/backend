package com.contextstt.backend.analysis.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenRouterChatResponse(String id, String model, List<OpenRouterChoice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenRouterChoice(int index, OpenRouterMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenRouterMessage(String role, String content) {
    }
}
