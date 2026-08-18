package com.contextstt.backend.service;

import com.contextstt.backend.analysis.ContextAnalysisGateway;
import com.contextstt.backend.domain.analysis.Analysis;
import com.contextstt.backend.domain.analysis.AnalysisRepository;
import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.domain.transcription.TranscriptionRepository;
import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.domain.user.UserRepository;
import com.contextstt.backend.dto.analysis.AnalysisStatusResponse;
import com.contextstt.backend.dto.analysis.AnalysisSubmitResponse;
import com.contextstt.backend.exception.AnalysisNotFoundException;
import com.contextstt.backend.exception.TranscriptionNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final UserRepository userRepository;
    private final ContextAnalysisGateway contextAnalysisGateway;
    private final AnalysisProcessor analysisProcessor;
    private final ObjectMapper objectMapper;

    public AnalysisSubmitResponse submit(Long userId, Long transcriptionId) {
        Transcription transcription = transcriptionRepository.findByIdAndUserId(transcriptionId, userId)
                .orElseThrow(TranscriptionNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Analysis analysis = Analysis.builder()
                .user(user)
                .transcription(transcription)
                .provider(contextAnalysisGateway.provider())
                .model(contextAnalysisGateway.model())
                .build();
        analysis = analysisRepository.saveAndFlush(analysis);

        analysisProcessor.process(analysis.getId(), transcription.getCurrentText());

        return AnalysisSubmitResponse.from(analysis);
    }

    @Transactional(readOnly = true)
    public AnalysisStatusResponse get(Long userId, Long analysisId) {
        Analysis analysis = analysisRepository.findByIdAndUserId(analysisId, userId)
                .orElseThrow(AnalysisNotFoundException::new);
        return AnalysisStatusResponse.from(analysis, objectMapper);
    }
}
