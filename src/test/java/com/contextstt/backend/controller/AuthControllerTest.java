package com.contextstt.backend.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signupThenLoginAndAccessProtectedEndpointSucceeds() throws Exception {
        String signupPayload = objectMapper.writeValueAsString(
                new SignupPayload("test@contextstt.com", "password1", "테스터")
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@contextstt.com"));

        String loginPayload = objectMapper.writeValueAsString(
                new LoginPayload("test@contextstt.com", "password1")
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.user.email").value("test@contextstt.com"))
                .andReturn();

        String accessToken = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token.accessToken"
        );

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@contextstt.com"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@contextstt.com"));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        String signupPayload = objectMapper.writeValueAsString(
                new SignupPayload("wrong@contextstt.com", "password1", "테스터2")
        );
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isCreated());

        String loginPayload = objectMapper.writeValueAsString(
                new LoginPayload("wrong@contextstt.com", "wrongpassword")
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateSignupReturnsConflict() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new SignupPayload("duplicate@contextstt.com", "password1", "중복가입")
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("이미 가입된 이메일입니다: duplicate@contextstt.com"));
    }

    @Test
    void loginWithUnknownEmailReturnsGenericUnauthorizedResponse() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new LoginPayload("unknown@contextstt.com", "password1")
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void protectedEndpointWithInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private record SignupPayload(String email, String password, String nickname) {
    }

    private record LoginPayload(String email, String password) {
    }
}