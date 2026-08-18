package com.contextstt.backend.service;

import com.contextstt.backend.domain.analysis.ContextAnalysis;
import com.contextstt.backend.domain.analysis.ContextAnalysisRepository;
import com.contextstt.backend.dto.conversation.ConversationUtteranceResolutionResponse;
import com.contextstt.backend.exception.ContextResolutionNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationContextResolutionService {

    private static final PageRequest LATEST_ONLY = PageRequest.of(0, 1);

    private final ContextAnalysisRepository analysisRepository;

    @Transactional(readOnly = true)
    public ConversationUtteranceResolutionResponse getLatest(
            Long ownerId,
            Long conversationId,
            Long utteranceId
    ) {
        List<Long> analysisIds = analysisRepository.findUsableAnalysisIds(
                conversationId,
                utteranceId,
                ownerId,
                LATEST_ONLY
        );
        if (analysisIds.isEmpty()) {
            throw new ContextResolutionNotFoundException();
        }

        ContextAnalysis analysis = analysisRepository
                .findDetailedByIdAndOwnerId(analysisIds.getFirst(), ownerId)
                .filter(ContextAnalysis::hasUsableResolution)
                .orElseThrow(ContextResolutionNotFoundException::new);
        return ConversationUtteranceResolutionResponse.from(analysis);
    }
}
