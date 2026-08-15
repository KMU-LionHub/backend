package com.contextstt.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsDefineBearerAuthenticationWithoutGlobalSecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security").doesNotExist())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void authenticationOperationsArePublicAndDocumentTheirResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/auth/signup']['post']['security']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/auth/signup']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/signup']['post']['responses']['400']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/signup']['post']['responses']['409']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']['security']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']['responses']['200']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']['responses']['400']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']['responses']['401']").exists());
    }

    @Test
    void currentUserOperationRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/users/me']['get']['security'][0].bearerAuth").isArray())
                .andExpect(jsonPath("$['paths']['/api/users/me']['get']['responses']['200']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me']['get']['responses']['401']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me']['get']['responses']['404']").exists());
    }

    @Test
    void requestSchemasExposeUsableExamples() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.SignupRequest.default").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.SignupRequest.properties.email.example")
                        .value("user@example.com"))
                .andExpect(jsonPath("$.components.schemas.SignupRequest.properties.password.example")
                        .value("password1"));
    }
}