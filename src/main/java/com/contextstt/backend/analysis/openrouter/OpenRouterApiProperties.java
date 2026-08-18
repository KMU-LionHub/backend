package com.contextstt.backend.analysis.openrouter;

import com.contextstt.backend.analysis.ContextAnalysisModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "openrouter.api")
public class OpenRouterApiProperties {

    private boolean enabled;

    private String apiKey;

    @NotBlank
    private String baseUrl = "https://openrouter.ai/api/v1";

    @NotBlank
    private String geminiModel = "google/gemini-3.7-flash";

    @NotBlank
    private String deepseekModel = "deepseek/deepseek-v4-flash";

    @Positive
    private int maxTokens = 2048;

    @Positive
    private int timeoutSeconds = 30;

    private String siteUrl;

    @NotBlank
    private String appTitle = "Context STT";

    public String modelId(ContextAnalysisModel model) {
        return switch (model) {
            case GEMINI_3_7_FLASH -> geminiModel;
            case DEEPSEEK_V4_FLASH -> deepseekModel;
            case CLAUDE_SONNET_5 -> throw new IllegalArgumentException(
                    "Claude 모델은 OpenRouter 분석 제공자가 처리할 수 없습니다."
            );
        };
    }
}
