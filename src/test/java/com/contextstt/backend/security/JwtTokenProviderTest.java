package com.contextstt.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.io.DecodingException;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String BASE64_SECRET =
            "Y29udGV4dC1zdHQtdGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LTMyLWJ5dGVz";

    @Test
    void createsAndValidatesTokenWithBase64Secret() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(BASE64_SECRET, 3_600_000));

        String token = provider.createAccessToken(1L, "test@contextstt.com");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    void rejectsNonBase64Secret() {
        JwtProperties properties = new JwtProperties("not-a-base64-secret", 3_600_000);

        assertThatThrownBy(() -> new JwtTokenProvider(properties))
                .isInstanceOf(DecodingException.class);
    }
}