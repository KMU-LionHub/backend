package com.contextstt.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String BASE64_SECRET =
            "Y29udGV4dC1zdHQtdGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LTMyLWJ5dGVz";

    @Test
    void createsAndValidatesTokenWithBase64Secret() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(BASE64_SECRET, 3_600_000));

        String token = provider.createAccessToken(1L, "test@contextstt.com");

        assertThat(provider.extractUserId(token)).contains(1L);
    }

    @Test
    void rejectsSignedTokenWithNonNumericSubject() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(BASE64_SECRET, 3_600_000));
        String token = Jwts.builder()
                .subject("not-a-number")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_SECRET)))
                .compact();

        assertThat(provider.extractUserId(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(BASE64_SECRET, 3_600_000));
        String token = Jwts.builder()
                .subject("1")
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_SECRET)))
                .compact();

        assertThat(provider.extractUserId(token)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(BASE64_SECRET, 3_600_000));
        byte[] otherSecret = "another-test-secret-key-at-least-32-bytes"
                .getBytes(StandardCharsets.UTF_8);
        String token = Jwts.builder()
                .subject("1")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(otherSecret))
                .compact();

        assertThat(provider.extractUserId(token)).isEmpty();
    }

    @Test
    void rejectsNonBase64Secret() {
        JwtProperties properties = new JwtProperties("not-a-base64-secret", 3_600_000);

        assertThatThrownBy(() -> new JwtTokenProvider(properties))
                .isInstanceOf(DecodingException.class);
    }
}