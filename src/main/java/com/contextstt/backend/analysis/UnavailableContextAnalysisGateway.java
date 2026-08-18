package com.contextstt.backend.analysis;

import com.contextstt.backend.exception.AnalysisProviderUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class UnavailableContextAnalysisGateway implements ContextAnalysisGateway {

    @Override
    public ContextAnalysisResult analyze(ContextAnalysisInput input, int candidateCount) {
        throw new AnalysisProviderUnavailableException("맥락 분석 제공자가 설정되어 있지 않습니다.");
    }
}
