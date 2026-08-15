package com.contextstt.backend.security;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
        "auth.rate-limit.signup.capacity=2",
        "auth.rate-limit.login.capacity=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRateLimitFilterTest {

    private static final String RATE_LIMIT_MESSAGE = "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signupRateLimitCountsInvalidRequests() throws Exception {
        MockHttpServletRequestBuilder request = requestFrom("/api/auth/signup", "{}", "192.0.2.10");

        mockMvc.perform(request).andExpect(status().isBadRequest());
        mockMvc.perform(request).andExpect(status().isBadRequest());

        mockMvc.perform(request)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("[1-9]\\d*")))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(RATE_LIMIT_MESSAGE));
    }

    @Test
    void loginRateLimitReturnsTooManyRequestsAfterCapacity() throws Exception {
        String payload = """
                {
                  "email": "unknown@contextstt.com",
                  "password": "password1"
                }
                """;
        MockHttpServletRequestBuilder request = requestFrom("/api/auth/login", payload, "192.0.2.10");

        mockMvc.perform(request).andExpect(status().isUnauthorized());
        mockMvc.perform(request).andExpect(status().isUnauthorized());

        mockMvc.perform(request)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("[1-9]\\d*")))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(RATE_LIMIT_MESSAGE));
    }

    private MockHttpServletRequestBuilder requestFrom(String path, String payload, String remoteAddress) {
        return post(path)
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload);
    }
}