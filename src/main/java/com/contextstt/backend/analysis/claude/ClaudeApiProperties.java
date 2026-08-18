package com.contextstt.backend.analysis.claude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "claude.api")
public class ClaudeApiProperties {

    private boolean enabled;

    private String apiKey;

    @NotBlank
    private String baseUrl = "https://api.anthropic.com";

    @NotBlank
    private String model = "claude-sonnet-5";

    @Positive
    private int maxTokens = 1024;

    @Positive
    private int timeoutSeconds = 30;
}
