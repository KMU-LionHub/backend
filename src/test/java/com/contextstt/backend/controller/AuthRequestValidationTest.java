package com.contextstt.backend.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @Test
    void signupAllowsVisibleAsciiSpecialCharactersInPassword() throws Exception {
        String payload = """
                {
                  "email": "ascii-password@contextstt.com",
                  "password": "password1!~",
                  "nickname": "아스키"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"password1한글", "password 1"})
    void signupRejectsPasswordOutsideVisibleAscii(String password) throws Exception {
        String payload = """
                {
                  "email": "invalid-password@contextstt.com",
                  "password": "%s",
                  "nickname": "검증사용자"
                }
                """.formatted(password);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem(
                        "password: 비밀번호는 공백 없이 영문, 숫자, 특수문자만 사용할 수 있습니다."
                )));
    }

    @Test
    void loginRejectsNonAsciiPassword() throws Exception {
        String payload = """
                {
                  "email": "test@contextstt.com",
                  "password": "password1한글"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem(
                        "password: 비밀번호는 공백 없이 영문, 숫자, 특수문자만 사용할 수 있습니다."
                )));
    }

    @Test
    void signupRejectsInvalidEmailPasswordAndNickname() throws Exception {
        String payload = """
                {
                  "email": "invalid-email",
                  "password": "password",
                  "nickname": "x"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItems(
                        "email: 이메일 형식이 올바르지 않습니다.",
                        "password: 비밀번호는 영문과 숫자를 포함해야 합니다.",
                        "nickname: 닉네임은 2자 이상 20자 이하이어야 합니다."
                )));
    }
}