package com.contextstt.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void acceptsOnlyHttpOriginsWithoutPathsOrWildcards() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of(
                " http://localhost:5173 ",
                "https://app.example.com",
                "http://localhost:5173"
        ));

        assertThat(properties.isAllowedOriginsValid()).isTrue();
        assertThat(properties.normalizedAllowedOrigins())
                .containsExactly("http://localhost:5173", "https://app.example.com");

        properties.setAllowedOrigins(List.of("*"));
        assertThat(properties.isAllowedOriginsValid()).isFalse();

        properties.setAllowedOrigins(List.of("https://app.example.com/path"));
        assertThat(properties.isAllowedOriginsValid()).isFalse();
    }
}
