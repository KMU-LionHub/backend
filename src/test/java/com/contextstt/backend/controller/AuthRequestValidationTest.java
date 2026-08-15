package com.contextstt.backend.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRequestValidationTest {

    private static final String TOO_LONG_EMAIL = "a".repeat(89) + "@example.com";
    private static final String TOO_LONG_PASSWORD = "a".repeat(65);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signupRejectsEmailLongerThanDatabaseColumn() throws Exception {
        String payload = """
                {
                  "email": "%s",
                  "password": "password1",
                  "nickname": "테스터"
                }
                """.formatted(TOO_LONG_EMAIL);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem("email: 이메일은 100자 이하여야 합니다.")));
    }

    @Test
    void loginRejectsEmailLongerThanDatabaseColumn() throws Exception {
        String payload = """
                {
                  "email": "%s",
                  "password": "password1"
                }
                """.formatted(TOO_LONG_EMAIL);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem("email: 이메일은 100자 이하여야 합니다.")));
    }

    @Test
    void loginRejectsPasswordLongerThanSignupLimit() throws Exception {
        String payload = """
                {
                  "email": "test@contextstt.com",
                  "password": "%s"
                }
                """.formatted(TOO_LONG_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem("password: 비밀번호는 64자 이하여야 합니다.")));
    }
}