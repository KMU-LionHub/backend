package com.contextstt.backend.analysis.prompt;

import java.util.List;
import java.util.Map;

public final class ContextAnalysisJsonSchema {

    private static final int MAX_AMBIGUITY_COUNT = 5;

    private ContextAnalysisJsonSchema() {
    }

    public static Map<String, Object> forCandidateCount(int candidateCount) {
        Map<String, Object> candidateProperties = Map.of(
                "interpretation", stringProperty("가능한 발언 해석"),
                "inferredIntent", stringProperty("추론한 화자의 의도"),
                "rationale", stringProperty("대화 정보에 근거한 판단 이유"),
                "intentSimilarityScore", Map.of(
                        "type", "number",
                        "description", "후보 간 비교용 의도 유사도 점수",
                        "minimum", 0,
                        "maximum", 1
                )
        );
        Map<String, Object> candidate = Map.of(
                "type", "object",
                "properties", candidateProperties,
                "required", List.of(
                        "interpretation",
                        "inferredIntent",
                        "rationale",
                        "intentSimilarityScore"
                ),
                "additionalProperties", false
        );
        Map<String, Object> candidates = Map.of(
                "type", "array",
                "items", candidate,
                "minItems", candidateCount,
                "maxItems", candidateCount
        );
        Map<String, Object> ambiguity = Map.of(
                "type", "object",
                "properties", Map.of(
                        "startWordOrder", Map.of("type", "integer", "minimum", 0),
                        "endWordOrder", Map.of("type", "integer", "minimum", 0),
                        "candidates", candidates
                ),
                "required", List.of("startWordOrder", "endWordOrder", "candidates"),
                "additionalProperties", false
        );
        Map<String, Object> ambiguities = Map.of(
                "type", "array",
                "items", ambiguity,
                "minItems", 0,
                "maxItems", MAX_AMBIGUITY_COUNT
        );
        return Map.of(
                "type", "object",
                "properties", Map.of("ambiguities", ambiguities),
                "required", List.of("ambiguities"),
                "additionalProperties", false
        );
    }

    private static Map<String, Object> stringProperty(String description) {
        return Map.of(
                "type", "string",
                "description", description,
                "minLength", 1
        );
    }
}
