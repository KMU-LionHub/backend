package com.contextstt.backend.analysis.guardrail;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "context-analysis.guardrails")
public class ContextAnalysisGuardrailProperties {

    @Min(1)
    private int maxPreviousUtterances = 20;

    @Min(1)
    private int maxPreviousCharacters = 12000;

    @Valid
    @NotNull
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {

        @Min(1)
        private int capacity = 10;

        @NotNull
        private Duration window = Duration.ofMinutes(1);

        @Min(1)
        private int maximumTrackedUsers = 10000;
    }
}
