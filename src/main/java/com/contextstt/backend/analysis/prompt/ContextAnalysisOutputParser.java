package com.contextstt.backend.analysis.prompt;

import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.analysis.GeneratedContextCandidate;
import com.contextstt.backend.exception.InvalidAnalysisResultException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ContextAnalysisOutputParser {

    private final ObjectMapper objectMapper;

    public ContextAnalysisResult parse(String text, String provider, String model) {
        if (!StringUtils.hasText(text)) {
            throw new InvalidAnalysisResultException("AI 분석 응답이 비어 있습니다.");
        }

        ContextAnalysisPayload payload;
        try {
            payload = objectMapper.readValue(stripCodeFence(text), ContextAnalysisPayload.class);
        } catch (JacksonException ex) {
            throw new InvalidAnalysisResultException("AI 분석 응답을 해석할 수 없습니다.", ex);
        }
        if (payload == null) {
            throw new InvalidAnalysisResultException("AI 분석 응답이 비어 있습니다.");
        }
        return new ContextAnalysisResult(provider, model, payload.candidates());
    }

    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            trimmed = trimmed.replaceFirst("```\\s*$", "");
        }
        return trimmed.trim();
    }

    private record ContextAnalysisPayload(List<GeneratedContextCandidate> candidates) {
    }
}
