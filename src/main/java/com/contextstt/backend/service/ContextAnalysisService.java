package com.contextstt.backend.service;

import com.contextstt.backend.analysis.ContextAnalysisGateway;
import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.analysis.ContextAnalysisSource;
import com.contextstt.backend.analysis.ContextAnalysisSourceLoader;
import com.contextstt.backend.analysis.GeneratedContextCandidate;
import com.contextstt.backend.domain.analysis.ContextAnalysis;
import com.contextstt.backend.domain.analysis.ContextAnalysisRepository;
import com.contextstt.backend.dto.analysis.ContextAnalysisHistoryResponse;
import com.contextstt.backend.dto.analysis.ContextAnalysisResponse;
import com.contextstt.backend.dto.analysis.ContextAnalysisSummaryResponse;
import com.contextstt.backend.dto.analysis.CreateContextAnalysisRequest;
import com.contextstt.backend.dto.analysis.EditContextSelectionRequest;
import com.contextstt.backend.dto.analysis.SelectContextCandidateRequest;
import com.contextstt.backend.exception.ContextAnalysisNotFoundException;
import com.contextstt.backend.exception.InvalidAnalysisResultException;
import com.contextstt.backend.exception.InvalidRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContextAnalysisService {

    private static final int DEFAULT_CANDIDATE_COUNT = 3;
    private static final int MIN_CANDIDATE_COUNT = 2;
    private static final int MAX_CANDIDATE_COUNT = 5;
    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.ONE;

    private final ContextAnalysisSourceLoader sourceLoader;
    private final ContextAnalysisGateway analysisGateway;
    private final ContextAnalysisRepository analysisRepository;

    public ContextAnalysisResponse analyze(Long ownerId, CreateContextAnalysisRequest request) {
        int candidateCount = resolveCandidateCount(request.candidateCount());
        ContextAnalysisSource source = sourceLoader.load(
                ownerId,
                request.conversationId(),
                request.utteranceId()
        );
        ContextAnalysisResult result = analysisGateway.analyze(source.input(), candidateCount);
        List<GeneratedContextCandidate> candidates = validateAndRank(result, candidateCount);

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
                .build();

        for (int index = 0; index < candidates.size(); index++) {
            GeneratedContextCandidate candidate = candidates.get(index);
            analysis.addCandidate(
                    index + 1,
                    candidate.interpretation().trim(),
                    candidate.inferredIntent().trim(),
                    candidate.rationale().trim(),
                    candidate.intentSimilarityScore().setScale(4, RoundingMode.HALF_UP)
            );
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
            SelectContextCandidateRequest request
    ) {
        ContextAnalysis analysis = findOwnedAnalysis(analysisId, ownerId);
        analysis.selectCandidate(request.candidateId());
        analysisRepository.flush();
        return ContextAnalysisResponse.from(analysis);
    }

    @Transactional
    public ContextAnalysisResponse editSelection(
            Long ownerId,
            Long analysisId,
            EditContextSelectionRequest request
    ) {
        ContextAnalysis analysis = findOwnedAnalysis(analysisId, ownerId);
        analysis.editSelection(request.text().trim());
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

    private List<GeneratedContextCandidate> validateAndRank(
            ContextAnalysisResult result,
            int candidateCount
    ) {
        if (result == null
                || !StringUtils.hasText(result.provider())
                || result.provider().length() > 50
                || !StringUtils.hasText(result.model())
                || result.model().length() > 100
                || result.candidates() == null
                || result.candidates().size() < candidateCount) {
            throw invalidProviderResult();
        }

        result.candidates().forEach(this::validateCandidate);
        return result.candidates().stream()
                .sorted(Comparator.comparing(
                        GeneratedContextCandidate::intentSimilarityScore,
                        Comparator.reverseOrder()
                ))
                .limit(candidateCount)
                .toList();
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
}
