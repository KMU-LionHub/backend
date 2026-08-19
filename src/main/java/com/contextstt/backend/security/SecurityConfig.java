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
                .cors(cors -> {})

                // REST API이므로 CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 기본 로그인 방식 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // JWT 사용 -> 세션 사용하지 않음
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                // 인증/인가 예외 처리
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(
                                                apiSecurityExceptionHandler
                                        )
                                        .accessDeniedHandler(
                                                apiSecurityExceptionHandler
                                        )
                )

                // URL별 접근 권한
                .authorizeHttpRequests(auth -> {

                    // 회원가입 / 로그인은 인증 없이 접근 가능
                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/auth/signup",
                            "/api/auth/login"
                    ).permitAll();

                    // Swagger 사용 허용
                    if (openApiProperties.enabled()) {
                        auth.requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll();
                    }

                    // 나머지는 로그인 필요
                    auth.anyRequest().authenticated();
                })

                // JWT 인증 필터
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // 로그인/회원가입 Rate Limit 필터
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

        // Vite 프론트엔드 주소 허용
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:5174"
                )
        );

        // 프론트에서 사용할 HTTP Method
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

        // Authorization, Content-Type 등 허용
        configuration.setAllowedHeaders(
                List.of("*")
        );

        // 응답에서 Authorization 헤더 접근 허용
        configuration.setExposedHeaders(
                List.of("Authorization")
        );

        // 인증정보 포함 요청 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // 모든 API 경로에 CORS 적용
        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}