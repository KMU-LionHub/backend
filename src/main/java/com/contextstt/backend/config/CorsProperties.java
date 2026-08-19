package com.contextstt.backend.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<@NotBlank String> allowedOrigins = List.of();

    public List<String> normalizedAllowedOrigins() {
        return allowedOrigins.stream()
                .map(String::trim)
                .distinct()
                .toList();
    }

    @AssertTrue(message = "CORS origin은 경로가 없는 http 또는 https origin이어야 합니다.")
    public boolean isAllowedOriginsValid() {
        return allowedOrigins.stream().allMatch(this::isValidOrigin);
    }

    private boolean isValidOrigin(String origin) {
        try {
            URI uri = URI.create(origin.trim());
            String scheme = uri.getScheme();
            String path = uri.getRawPath();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && (path == null || path.isEmpty() || "/".equals(path))
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
