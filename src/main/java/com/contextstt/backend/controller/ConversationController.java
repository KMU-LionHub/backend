package com.contextstt.backend.controller;

import com.contextstt.backend.config.OpenApiConfig;
import com.contextstt.backend.dto.conversation.AddUtteranceRequest;
import com.contextstt.backend.dto.conversation.ConversationPageResponse;
import com.contextstt.backend.dto.conversation.ConversationParticipantResponse;
import com.contextstt.backend.dto.conversation.ConversationResponse;
import com.contextstt.backend.dto.conversation.ConversationUtteranceResponse;
import com.contextstt.backend.dto.conversation.ConversationUtteranceResolutionResponse;
import com.contextstt.backend.dto.conversation.CreateConversationRequest;
import com.contextstt.backend.dto.conversation.CreateParticipantRequest;
import com.contextstt.backend.dto.conversation.ReplaceUtteranceTranscriptionRequest;
import com.contextstt.backend.exception.ErrorResponse;
import com.contextstt.backend.security.CustomUserDetails;
import com.contextstt.backend.service.ConversationService;
import com.contextstt.backend.service.ConversationContextResolutionService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대화", description = "대화 세션, 참여자 및 발언 관리 API")
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationContextResolutionService contextResolutionService;

    @Operation(
            summary = "대화 생성",
            description = "로그인 사용자를 SELF 참여자로 포함한 새 대화를 생성합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "중복 참여자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ConversationResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        ConversationResponse response = conversationService.create(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "내 대화 목록 조회",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "페이지 입력값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ConversationPageResponse list(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return conversationService.list(principal.getUserId(), page, size);
    }

    @Operation(
            summary = "대화 상세 조회",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{conversationId}")
    public ConversationResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long conversationId
    ) {
        return conversationService.get(principal.getUserId(), conversationId);
    }

    @Operation(
            summary = "상대 참여자 추가",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "추가 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationParticipantResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "종료된 대화 또는 중복 참여자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{conversationId}/participants")
    public ResponseEntity<ConversationParticipantResponse> addParticipant(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long conversationId,
            @Valid @RequestBody CreateParticipantRequest request
    ) {
        ConversationParticipantResponse response = conversationService.addParticipant(
                principal.getUserId(),
                conversationId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "전사를 대화 발언으로 연결",
            description = "사용자 소유 전사와 대화 참여자를 발언 순서에 맞게 연결합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "연결 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationUtteranceResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화, 참여자 또는 전사 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "종료된 대화 또는 이미 연결된 전사",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{conversationId}/utterances")
    public ResponseEntity<ConversationUtteranceResponse> addUtterance(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long conversationId,
            @Valid @RequestBody AddUtteranceRequest request
    ) {
        ConversationUtteranceResponse response = conversationService.addUtterance(
                principal.getUserId(),
                conversationId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "재발언 전사로 교체",
            description = "확정 전 발언을 STT 재발언 API로 생성한 새 전사로 교체합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "교체 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationUtteranceResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화, 발언 또는 전사 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "교체할 수 없는 전사 또는 발언 상태",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{conversationId}/utterances/{utteranceId}/transcription")
    public ConversationUtteranceResponse replaceUtteranceTranscription(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long conversationId,
            @PathVariable Long utteranceId,
            @Valid @RequestBody ReplaceUtteranceTranscriptionRequest request
    ) {
        return conversationService.replaceUtteranceTranscription(
                principal.getUserId(),
                conversationId,
                utteranceId,
                request
        );
    }

    @Operation(
            summary = "대화 발언 확정",
            description = "현재 교정 문장을 화자의 최종 발언으로 확정합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확정 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationUtteranceResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화 또는 발언 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "종료된 대화 또는 확정할 수 없는 전사",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{conversationId}/utterances/{utteranceId}/confirm")
    public ConversationUtteranceResponse confirmUtterance(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long conversationId,
            @PathVariable Long utteranceId
    ) {
        return conversationService.confirmUtterance(principal.getUserId(), conversationId, utteranceId);
    }

    @Operation(
            summary = "발언의 최신 확정 맥락 조회",
            description = "현재 전사와 일치하고 모든 모호성 구간이 확정된 최신 분석 결과를 반환합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationUtteranceResolutionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화, 발언 또는 사용 가능한 확정 맥락 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{conversationId}/utterances/{utteranceId}/resolution")
    public ConversationUtteranceResolutionResponse getUtteranceResolution(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long conversationId,
            @PathVariable Long utteranceId
    ) {
        return contextResolutionService.getLatest(
                principal.getUserId(),
                conversationId,
                utteranceId
        );
    }

    @Operation(
            summary = "대화 종료",
            description = "모든 발언이 확정된 대화를 종료합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "종료 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "발언 없음 또는 미확정 발언 존재",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{conversationId}/close")
    public ConversationResponse close(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long conversationId
    ) {
        return conversationService.close(principal.getUserId(), conversationId);
    }
}
