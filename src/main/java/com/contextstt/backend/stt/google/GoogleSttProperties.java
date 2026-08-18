package com.contextstt.backend.stt.google;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "stt.google")
public class GoogleSttProperties {

    public static final long GOOGLE_INLINE_AUDIO_LIMIT_BYTES = 10L * 1024 * 1024;

    private boolean enabled;

    private String projectId;

    @NotBlank
    private String location = "asia-northeast1";

    @NotBlank
    private String model = "long";

    @Positive
    @Max(GOOGLE_INLINE_AUDIO_LIMIT_BYTES)
    private long maxAudioBytes = GOOGLE_INLINE_AUDIO_LIMIT_BYTES;

    public String recognizerName() {
        return "projects/%s/locations/%s/recognizers/_".formatted(projectId.trim(), location.trim());
    }

    public String apiEndpoint() {
        String normalizedLocation = location.trim();
        if ("global".equalsIgnoreCase(normalizedLocation)) {
            return "speech.googleapis.com:443";
        }
        return normalizedLocation + "-speech.googleapis.com:443";
    }
}
