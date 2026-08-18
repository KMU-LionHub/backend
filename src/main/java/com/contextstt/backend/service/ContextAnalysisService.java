package com.contextstt.backend.service;

import com.contextstt.backend.analysis.ContextAnalysisGateway;
import com.contextstt.backend.analysis.ContextAnalysisModel;
import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.analysis.ContextAnalysisSource;
import com.contextstt.backend.analysis.ContextAnalysisSourceLoader;
import com.contextstt.backend.analysis.GeneratedContextAmbiguity;
import com.contextstt.backend.analysis.GeneratedContextCandidate;
import com.contextstt.backend.analysis.guardrail.ContextAnalysisRequestRateLimiter;
import com.contextstt.backend.analysis.guardrail.ContextAnalysisRequestRateLimiter.RateLimitDecision;
import com.contextstt.backend.domain.analysis.ContextAnalysis;
import com.contextstt.backend.domain.analysis.ContextAmbiguity;
import com.contextstt.backend.domain.analysis.ContextAnalysisRepository;
import com.contextstt.backend.domain.transcription.TranscriptWord;
import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.dto.analysis.ContextAnalysisHistoryResponse;
import com.contextstt.backend.dto.analysis.ContextAnalysisResponse;
import com.contextstt.backend.dto.analysis.ContextAnalysisSummaryResponse;
import com.contextstt.backend.dto.analysis.CreateContextAnalysisRequest;
import com.contextstt.backend.dto.analysis.EditContextSelectionRequest;
import com.contextstt.backend.dto.analysis.ResolveContextAmbiguityRequest;
import com.contextstt.backend.dto.analysis.SelectContextCandidateRequest;
import com.contextstt.backend.exception.ContextAnalysisNotFoundException;
import com.contextstt.backend.exception.ContextAnalysisRateLimitExceededException;
import com.contextstt.backend.exception.InvalidAnalysisResultException;
import com.contextstt.backend.exception.InvalidRequestException;
import com.contextstt.backend.exception.ResourceConflictException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContextAnalysisService {

    private static final ContextAnalysisModel DEFAULT_MODEL = ContextAnalysisModel.CLAUDE_SONNET_5;
    private static final int DEFAULT_CANDIDATE_COUNT = 3;
    private static final int MIN_CANDIDATE_COUNT = 2;
    private static final int MAX_CANDIDATE_COUNT = 5;
    private static final int MAX_AMBIGUITY_COUNT = 5;
    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.ONE;

    private final ContextAnalysisSourceLoader sourceLoader;
    private final ContextAnalysisGateway analysisGateway;
    private final ContextAnalysisRepository analysisRepository;
    private final ContextAnalysisRequestRateLimiter rateLimiter;

    public ContextAnalysisResponse analyze(Long ownerId, CreateContextAnalysisRequest request) {
        int candidateCount = resolveCandidateCount(request.candidateCount());
        ContextAnalysisModel model = request.model() == null ? DEFAULT_MODEL : request.model();
        ContextAnalysisSource source = sourceLoader.load(
                ownerId,
                request.conversationId(),
                request.utteranceId()
        );
        RateLimitDecision rateLimit = rateLimiter.tryAcquire(ownerId);
        if (!rateLimit.allowed()) {
            throw new ContextAnalysisRateLimitExceededException(rateLimit.retryAfterSeconds());
        }
        ContextAnalysisResult result = analysisGateway.analyze(source.input(), candidateCount, model);
        List<ValidatedAmbiguity> ambiguities = validateAndRank(
                result,
                candidateCount,
                source.transcription()
        );

        ContextAnalysis analysis = ContextAnalysis.builder()
                .conversation(source.conversation())
                .utterance(source.utterance())
                .transcription(source.transcription())
                .provider(result.provider().trim())
                .model(result.model().trim())
                .sourceSpeakerName(source.input().targetSpeakerName())
                .conversationContext(source.input().conversationContext())
                .sourceOriginalText(source.input().targetOriginalText())
                .sourceCurrentText(source.input().targetCurrentText())
                .requestedCandidateCount(candidateCount)
                .build();

        for (int ambiguityIndex = 0; ambiguityIndex < ambiguities.size(); ambiguityIndex++) {
            ValidatedAmbiguity validated = ambiguities.get(ambiguityIndex);
            ContextAmbiguity ambiguity = analysis.addAmbiguity(
                    ambiguityIndex + 1,
                    validated.excerpt(),
                    validated.startWord().getId(),
                    validated.endWord().getId(),
                    validated.startWord().getWordOrder(),
                    validated.endWord().getWordOrder()
            );
            for (int candidateIndex = 0; candidateIndex < validated.candidates().size(); candidateIndex++) {
                GeneratedContextCandidate candidate = validated.candidates().get(candidateIndex);
                ambiguity.addCandidate(
                        candidateIndex + 1,
                        candidate.interpretation().trim(),
                        candidate.inferredIntent().trim(),
                        candidate.rationale().trim(),
                        candidate.intentSimilarityScore().setScale(4, RoundingMode.HALF_UP)
                );
            }
        }

        return ContextAnalysisResponse.from(analysisRepository.saveAndFlush(analysis));
    }

    @Transactional(readOnly = true)
    public ContextAnalysisResponse get(Long ownerId, Long analysisId) {
        return ContextAnalysisResponse.from(findOwnedAnalysis(analysisId, ownerId));
    }

    @Transactional(readOnly = true)
    public ContextAnalysisHistoryResponse history(
            Long ownerId,
            Long conversationId,
            Long utteranceId
    ) {
        sourceLoader.load(ownerId, conversationId, utteranceId);
        List<ContextAnalysisSummaryResponse> analyses = analysisRepository
                .findByConversationIdAndUtteranceIdAndConversationOwnerIdOrderByCreatedAtDesc(
                        conversationId,
                        utteranceId,
                        ownerId
                ).stream()
                .map(ContextAnalysisSummaryResponse::from)
                .toList();
        return ContextAnalysisHistoryResponse.builder()
                .conversationId(conversationId)
                .utteranceId(utteranceId)
                .analyses(analyses)
                .build();
    }

    @Transactional
    public ContextAnalysisResponse selectCandidate(
            Long ownerId,
            Long analysisId,
            Long ambiguityId,
            SelectContextCandidateRequest request
    ) {
        ContextAnalysis analysis = findOwnedAnalysis(analysisId, ownerId);
        ensureFresh(analysis);
        analysis.findAmbiguity(ambiguityId).selectCandidate(request.candidateId());
        analysisRepository.flush();
        return ContextAnalysisResponse.from(analysis);
    }

    @Transactional
    public ContextAnalysisResponse editSelection(
            Long ownerId,
            Long analysisId,
            Long ambiguityId,
            EditContextSelectionRequest request
    ) {
        ContextAnalysis analysis = findOwnedAnalysis(analysisId, ownerId);
        ensureFresh(analysis);
        analysis.findAmbiguity(ambiguityId).editSelection(request.text().trim());
        analysisRepository.flush();
        return ContextAnalysisResponse.from(analysis);
    }

    @Transactional
    public ContextAnalysisResponse resolve(
            Long ownerId,
            Long analysisId,
            Long ambiguityId,
            ResolveContextAmbiguityRequest request
    ) {
        ContextAnalysis analysis = findOwnedAnalysis(analysisId, ownerId);
        ensureFresh(analysis);
        ContextAmbiguity ambiguity = analysis.findAmbiguity(ambiguityId);

        switch (request.type()) {
            case CANDIDATE -> {
                if (request.candidateId() == null || StringUtils.hasText(request.text())) {
                    throw invalidResolutionRequest();
                }
                ambiguity.selectCandidate(request.candidateId());
            }
            case CUSTOM -> {
                if (request.candidateId() != null || !StringUtils.hasText(request.text())) {
                    throw invalidResolutionRequest();
                }
                ambiguity.resolveCustom(request.text().trim());
            }
            case DISMISSED -> {
                if (request.candidateId() != null || StringUtils.hasText(request.text())) {
                    throw invalidResolutionRequest();
                }
                ambiguity.dismiss();
            }
        }

        analysisRepository.flush();
        return ContextAnalysisResponse.from(analysis);
    }

    private ContextAnalysis findOwnedAnalysis(Long analysisId, Long ownerId) {
        return analysisRepository.findDetailedByIdAndOwnerId(analysisId, ownerId)
                .orElseThrow(ContextAnalysisNotFoundException::new);
    }

    private int resolveCandidateCount(Integer requestedCount) {
        int count = requestedCount == null ? DEFAULT_CANDIDATE_COUNT : requestedCount;
        if (count < MIN_CANDIDATE_COUNT || count > MAX_CANDIDATE_COUNT) {
            throw new InvalidRequestException("맥락 후보 수는 2개 이상 5개 이하여야 합니다.");
        }
        return count;
    }

    private void ensureFresh(ContextAnalysis analysis) {
        if (analysis.isStale()) {
            throw new ResourceConflictException(
                    "분석 이후 전사가 변경되었습니다. 현재 발언으로 다시 분석해 주세요."
            );
        }
    }

    private InvalidRequestException invalidResolutionRequest() {
        return new InvalidRequestException("확정 유형에 맞는 candidateId 또는 text를 입력해 주세요.");
    }

    private List<ValidatedAmbiguity> validateAndRank(
            ContextAnalysisResult result,
            int candidateCount,
            Transcription transcription
    ) {
        if (result == null
                || !StringUtils.hasText(result.provider())
                || result.provider().length() > 50
                || !StringUtils.hasText(result.model())
                || result.model().length() > 100
                || result.ambiguities() == null
                || result.ambiguities().size() > MAX_AMBIGUITY_COUNT) {
            throw invalidProviderResult();
        }

        Map<Integer, TranscriptWord> wordsByOrder = transcription.getWords().stream()
                .collect(Collectors.toMap(TranscriptWord::getWordOrder, Function.identity()));
        List<ValidatedAmbiguity> ambiguities = result.ambiguities().stream()
                .map(ambiguity -> validateAmbiguity(ambiguity, candidateCount, wordsByOrder))
                .sorted(Comparator.comparing(item -> item.startWord().getWordOrder()))
                .toList();

        int previousEndOrder = -1;
        for (ValidatedAmbiguity ambiguity : ambiguities) {
            if (ambiguity.startWord().getWordOrder() <= previousEndOrder) {
                throw invalidProviderResult();
            }
            previousEndOrder = ambiguity.endWord().getWordOrder();
        }
        return ambiguities;
    }

    private ValidatedAmbiguity validateAmbiguity(
            GeneratedContextAmbiguity ambiguity,
            int candidateCount,
            Map<Integer, TranscriptWord> wordsByOrder
    ) {
        if (ambiguity == null
                || ambiguity.startWordOrder() == null
                || ambiguity.endWordOrder() == null
                || ambiguity.startWordOrder() < 0
                || ambiguity.endWordOrder() < ambiguity.startWordOrder()
                || ambiguity.candidates() == null
                || ambiguity.candidates().size() < candidateCount) {
            throw invalidProviderResult();
        }

        TranscriptWord startWord = wordsByOrder.get(ambiguity.startWordOrder());
        TranscriptWord endWord = wordsByOrder.get(ambiguity.endWordOrder());
        if (startWord == null || endWord == null) {
            throw invalidProviderResult();
        }

        ambiguity.candidates().forEach(this::validateCandidate);
        List<GeneratedContextCandidate> candidates = ambiguity.candidates().stream()
                .sorted(Comparator.comparing(
                        GeneratedContextCandidate::intentSimilarityScore,
                        Comparator.reverseOrder()
                ))
                .limit(candidateCount)
                .toList();
        String excerpt = wordsByOrder.values().stream()
                .filter(word -> word.getWordOrder() >= ambiguity.startWordOrder())
                .filter(word -> word.getWordOrder() <= ambiguity.endWordOrder())
                .sorted(Comparator.comparing(TranscriptWord::getWordOrder))
                .map(TranscriptWord::getCurrentText)
                .collect(Collectors.joining(" "));
        if (!StringUtils.hasText(excerpt)) {
            throw invalidProviderResult();
        }
        return new ValidatedAmbiguity(startWord, endWord, excerpt, candidates);
    }

    private void validateCandidate(GeneratedContextCandidate candidate) {
        if (candidate == null
                || !StringUtils.hasText(candidate.interpretation())
                || !StringUtils.hasText(candidate.inferredIntent())
                || !StringUtils.hasText(candidate.rationale())
                || candidate.intentSimilarityScore() == null
                || candidate.intentSimilarityScore().compareTo(MIN_SCORE) < 0
                || candidate.intentSimilarityScore().compareTo(MAX_SCORE) > 0) {
            throw invalidProviderResult();
        }
    }

    private InvalidAnalysisResultException invalidProviderResult() {
        return new InvalidAnalysisResultException("맥락 분석 제공자가 올바르지 않은 결과를 반환했습니다.");
    }

    private record ValidatedAmbiguity(
            TranscriptWord startWord,
            TranscriptWord endWord,
            String excerpt,
            List<GeneratedContextCandidate> candidates
    ) {
    }
}
