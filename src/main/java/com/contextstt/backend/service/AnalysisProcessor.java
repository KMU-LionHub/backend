package com.contextstt.backend.service;

import com.contextstt.backend.analysis.ContextAnalysisGateway;
import com.contextstt.backend.analysis.ContextAnalysisResult;
import com.contextstt.backend.domain.analysis.Analysis;
import com.contextstt.backend.domain.analysis.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisProcessor {

    private static final int PROGRESS_STARTED = 10;
    private static final int PROGRESS_REQUEST_SENT = 40;
    private static final int PROGRESS_RESPONSE_RECEIVED = 90;

    private final AnalysisRepository analysisRepository;
    private final ContextAnalysisGateway contextAnalysisGateway;
    private final ObjectMapper objectMapper;

    @Async("analysisTaskExecutor")
    public void process(Long analysisId, String transcriptText) {
        updateProgress(analysisId, PROGRESS_STARTED);

        try {
            updateProgress(analysisId, PROGRESS_REQUEST_SENT);
            ContextAnalysisResult result = contextAnalysisGateway.analyze(transcriptText);

            updateProgress(analysisId, PROGRESS_RESPONSE_RECEIVED);
            String resultJson = objectMapper.writeValueAsString(result);

            Analysis analysis = analysisRepository.findById(analysisId).orElseThrow();
            analysis.markCompleted(resultJson);
            analysisRepository.save(analysis);
        } catch (RuntimeException ex) {
            log.warn("AI 맥락 분석 실패 (analysisId={})", analysisId, ex);
            Analysis analysis = analysisRepository.findById(analysisId).orElseThrow();
            analysis.markFailed(ex.getMessage() != null ? ex.getMessage() : "AI 분석 중 오류가 발생했습니다.");
            analysisRepository.save(analysis);
        }
    }

    private void updateProgress(Long analysisId, int progress) {
        Analysis analysis = analysisRepository.findById(analysisId).orElseThrow();
        analysis.markInProgress(progress);
        analysisRepository.save(analysis);
    }
}
