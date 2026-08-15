package com.contextstt.backend.security;

import com.contextstt.backend.exception.ApiErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

final class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String SIGNUP_PATH = "/api/auth/signup";
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String UNKNOWN_CLIENT = "unknown";
    private static final String RATE_LIMIT_MESSAGE = "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";

    private final AuthRequestRateLimiter rateLimiter;
    private final ApiErrorResponseWriter errorResponseWriter;

    public AuthRateLimitFilter(
            AuthRequestRateLimiter rateLimiter,
            ApiErrorResponseWriter errorResponseWriter
    ) {
        this.rateLimiter = rateLimiter;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        AuthEndpoint endpoint = resolveEndpoint(request);
        if (endpoint == null) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthRequestRateLimiter.RateLimitDecision decision =
                rateLimiter.tryAcquire(endpoint, resolveClientAddress(request));
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        errorResponseWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, RATE_LIMIT_MESSAGE);
    }

    private AuthEndpoint resolveEndpoint(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }

        return switch (requestPath(request)) {
            case SIGNUP_PATH -> AuthEndpoint.SIGNUP;
            case LOGIN_PATH -> AuthEndpoint.LOGIN;
            default -> null;
        };
    }

    private String requestPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private String resolveClientAddress(HttpServletRequest request) {
        return StringUtils.hasText(request.getRemoteAddr()) ? request.getRemoteAddr() : UNKNOWN_CLIENT;
    }
}