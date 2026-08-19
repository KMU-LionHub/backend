package com.contextstt.backend.security;

import com.contextstt.backend.config.OpenApiProperties;
import com.contextstt.backend.exception.ApiErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        AuthRateLimitProperties.class,
        OpenApiProperties.class
})
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiSecurityExceptionHandler apiSecurityExceptionHandler;
    private final AuthRequestRateLimiter authRequestRateLimiter;
    private final ApiErrorResponseWriter apiErrorResponseWriter;
    private final OpenApiProperties openApiProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        AuthRateLimitFilter authRateLimitFilter =
                new AuthRateLimitFilter(
                        authRequestRateLimiter,
                        apiErrorResponseWriter
                );

        http
                // CORS 활성화
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        apiSecurityExceptionHandler
                                )
                                .accessDeniedHandler(
                                        apiSecurityExceptionHandler
                                )
                )

                .authorizeHttpRequests(auth -> {

                    // 회원가입 / 로그인 허용
                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/auth/signup",
                            "/api/auth/login"
                    ).permitAll();

                    // Swagger
                    if (openApiProperties.enabled()) {
                        auth.requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll();
                    }

                    // 나머지 요청은 인증 필요
                    auth.anyRequest().authenticated();
                })

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterBefore(
                        authRateLimitFilter,
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }

    // ========================================
    // CORS 설정
    // ========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // Vite 개발 서버 허용
        // localhost:5173 ~ localhost:5179
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:5175",
                        "http://localhost:5176",
                        "http://localhost:5177",
                        "http://localhost:5178",
                        "http://localhost:5179"
                )
        );

        // 허용 HTTP Method
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // 모든 요청 헤더 허용
        configuration.setAllowedHeaders(
                List.of("*")
        );

        // Authorization 등의 응답 헤더 접근 허용
        configuration.setExposedHeaders(
                List.of("Authorization")
        );

        // 쿠키 / 인증정보 허용
        configuration.setAllowCredentials(true);

        // preflight 결과 캐시
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // ========================================
    // Password Encoder
    // ========================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}