package com.contextstt.backend.analysis.guardrail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ContextAnalysisGuardrailProperties.class)
public class ContextAnalysisGuardrailConfiguration {
}
