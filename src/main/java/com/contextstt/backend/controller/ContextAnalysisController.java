package com.contextstt.backend.controller;

import com.contextstt.backend.config.OpenApiConfig;
import com.contextstt.backend.dto.analysis.ContextAnalysisHistoryResponse;
import com.contextstt.backend.dto.analysis.ContextAnalysisResponse;
import com.contextstt.backend.dto.analysis.CreateContextAnalysisRequest;
import com.contextstt.backend.dto.analysis.EditContextSelectionRequest;
import com.contextstt.backend.dto.analysis.SelectContextCandidateRequest;
import com.contextstt.backend.exception.ErrorResponse;
import com.contextstt.backend.security.CustomUserDetails;
import com.contextstt.backend.service.ContextAnalysisService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "맥락 분석", description = "발언 맥락 후보 생성, 조회 및 화자 확정 API")
@RestController
@RequestMapping("/api/context-analyses")
@RequiredArgsConstructor
public class ContextAnalysisController {

    private final ContextAnalysisService analysisService;

    @Operation(
            summary = "확정 발언 맥락 분석",
            description = "이전 확정 발언과 대화 배경을 바탕으로 복수의 맥락 후보를 생성합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "분석 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContextAnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화 또는 발언 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "확정되지 않은 발언",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "분석 제공자의 잘못된 응답",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "분석 제공자 미설정 또는 장애",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ContextAnalysisResponse> analyze(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateContextAnalysisRequest request
    ) {
        ContextAnalysisResponse response = analysisService.analyze(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "맥락 분석 상세 조회",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContextAnalysisResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 결과 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{analysisId}")
    public ContextAnalysisResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long analysisId
    ) {
        return analysisService.get(principal.getUserId(), analysisId);
    }

    @Operation(
            summary = "발언별 분석 이력 조회",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContextAnalysisHistoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화 또는 발언 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "확정되지 않은 발언",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ContextAnalysisHistoryResponse history(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long conversationId,
            @RequestParam Long utteranceId
    ) {
        return analysisService.history(principal.getUserId(), conversationId, utteranceId);
    }

    @Operation(
            summary = "맥락 후보 선택",
            description = "화자의 의도와 가장 가까운 후보를 최종 맥락의 기준으로 선택합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선택 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContextAnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 결과 또는 후보 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "동시 선택 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{analysisId}/selection")
    public ContextAnalysisResponse selectCandidate(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long analysisId,
            @Valid @RequestBody SelectContextCandidateRequest request
    ) {
        return analysisService.selectCandidate(principal.getUserId(), analysisId, request);
    }

    @Operation(
            summary = "선택 맥락 직접 수정",
            description = "선택한 후보를 화자가 원하는 표현으로 수정합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContextAnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 결과 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "선택된 후보 없음 또는 동시 수정 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{analysisId}/selection")
    public ContextAnalysisResponse editSelection(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long analysisId,
            @Valid @RequestBody EditContextSelectionRequest request
    ) {
        return analysisService.editSelection(principal.getUserId(), analysisId, request);
    }
}
