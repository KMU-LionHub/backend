package com.contextstt.backend.analysis.claude;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ClaudeApiProperties.class)
public class ClaudeConfiguration {

    @Bean
    public RestClient claudeRestClient(ClaudeApiProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }
}
