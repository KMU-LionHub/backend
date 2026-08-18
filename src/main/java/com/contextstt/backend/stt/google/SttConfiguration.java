package com.contextstt.backend.stt.google;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoogleSttProperties.class)
public class SttConfiguration {
}
