package com.contextstt.backend.controller;

import com.contextstt.backend.config.OpenApiConfig;
import com.contextstt.backend.dto.analysis.AnalysisStatusResponse;
import com.contextstt.backend.dto.analysis.AnalysisSubmitRequest;
import com.contextstt.backend.dto.analysis.AnalysisSubmitResponse;
import com.contextstt.backend.exception.ErrorResponse;
import com.contextstt.backend.security.CustomUserDetails;
import com.contextstt.backend.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 맥락 분석", description = "발언 전사 기반 AI 맥락 후보 분석 API")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(
            summary = "맥락 분석 요청",
            description = "전사 기록의 현재 문장을 Claude로 분석합니다. 분석은 비동기로 처리되며, 반환된 analysisId로 진행 상태를 조회할 수 있습니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "분석 요청 접수",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AnalysisSubmitResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "전사 기록 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "AI 분석 제공자 사용 불가",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AnalysisSubmitResponse> submit(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AnalysisSubmitRequest request
    ) {
        AnalysisSubmitResponse response = analysisService.submit(principal.getUserId(), request.transcriptionId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(
            summary = "맥락 분석 상태 조회",
            description = "분석 진행 상태와 진행률을 조회합니다. 완료 시 맥락 후보 목록이 함께 반환됩니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AnalysisStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 기록 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{analysisId}")
    public AnalysisStatusResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long analysisId
    ) {
        return analysisService.get(principal.getUserId(), analysisId);
    }
}
