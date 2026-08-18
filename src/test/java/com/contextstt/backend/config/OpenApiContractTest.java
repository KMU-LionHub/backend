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
                .andExpect(jsonPath("$['paths']['/api/auth/signup']['post']['responses']['429']").exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/auth/signup']['post']['responses']['429']['headers']['Retry-After']"
                ).exists())
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']['security']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']['responses']['200']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']['responses']['400']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']['responses']['401']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']['responses']['429']").exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/auth/login']['post']['responses']['429']['headers']['Retry-After']"
                ).exists());
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

    @Test
    void sttOperationsRequireAuthenticationAndDocumentExpectedFailures() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$['paths']['/api/stt/transcriptions']['post']['security'][0].bearerAuth"
                ).isArray())
                .andExpect(jsonPath("$['paths']['/api/stt/transcriptions']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/api/stt/transcriptions']['post']['responses']['413']").exists())
                .andExpect(jsonPath("$['paths']['/api/stt/transcriptions']['post']['responses']['422']").exists())
                .andExpect(jsonPath("$['paths']['/api/stt/transcriptions']['post']['responses']['503']").exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/stt/transcriptions']['post']['requestBody']['content']"
                                + "['multipart/form-data']['schema']['properties']['audio']['format']"
                ).value("binary"))
                .andExpect(jsonPath(
                        "$['paths']['/api/stt/transcriptions']['post']['responses']['201']['content']"
                                + "['application/json']['schema']['$ref']"
                ).value("#/components/schemas/TranscriptionResponse"))
                .andExpect(jsonPath(
                        "$['paths']['/api/stt/transcriptions/{transcriptionId}']['get']['security'][0].bearerAuth"
                ).isArray())
                .andExpect(jsonPath(
                        "$['paths']['/api/stt/transcriptions/{transcriptionId}/words/{wordId}']"
                                + "['patch']['responses']['409']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/stt/transcriptions/{transcriptionId}/re-record']"
                                + "['post']['responses']['201']"
                ).exists());
    }

    @Test
    void conversationOperationsRequireAuthenticationAndExposeSessionFlow() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$['paths']['/api/conversations']['post']['security'][0].bearerAuth"
                ).isArray())
                .andExpect(jsonPath("$['paths']['/api/conversations']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/api/conversations']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/conversations/{conversationId}']['get']").exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/conversations/{conversationId}/participants']['post']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/conversations/{conversationId}/participants']['post']['responses']['201']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/conversations/{conversationId}/utterances']['post']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/conversations/{conversationId}/utterances']['post']['responses']['201']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/conversations/{conversationId}/utterances/{utteranceId}/transcription']"
                                + "['patch']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/conversations/{conversationId}/utterances/{utteranceId}/confirm']"
                                + "['post']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/conversations/{conversationId}/close']['post']"
                ).exists());
    }

    @Test
    void contextAnalysisOperationsRequireAuthenticationAndExposeSelectionFlow() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$['paths']['/api/context-analyses']['post']['security'][0].bearerAuth"
                ).isArray())
                .andExpect(jsonPath(
                        "$['paths']['/api/context-analyses']['post']['responses']['201']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/context-analyses']['post']['responses']['502']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/context-analyses']['post']['responses']['503']"
                ).exists())
                .andExpect(jsonPath("$.components.schemas.CreateContextAnalysisRequest.properties.model.enum[0]")
                        .value("CLAUDE_SONNET_5"))
                .andExpect(jsonPath("$.components.schemas.CreateContextAnalysisRequest.properties.model.enum[1]")
                        .value("GEMINI_3_7_FLASH"))
                .andExpect(jsonPath("$.components.schemas.CreateContextAnalysisRequest.properties.model.enum[2]")
                        .value("DEEPSEEK_V4_FLASH"))
                .andExpect(jsonPath("$['paths']['/api/context-analyses']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/context-analyses/{analysisId}']['get']").exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/context-analyses/{analysisId}/selection']['put']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/context-analyses/{analysisId}/selection']['patch']"
                ).exists());
    }
}
