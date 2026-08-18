package com.contextstt.backend.analysis;

import com.contextstt.backend.exception.AnalysisProviderUnavailableException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoutingContextAnalysisGateway implements ContextAnalysisGateway {

    private final List<ContextAnalysisProvider> providers;

    @Override
    public ContextAnalysisResult analyze(
            ContextAnalysisInput input,
            int candidateCount,
            ContextAnalysisModel model
    ) {
        ContextAnalysisProvider provider = providers.stream()
                .filter(candidate -> candidate.supports(model))
                .findFirst()
                .orElseThrow(() -> new AnalysisProviderUnavailableException(
                        "선택한 맥락 분석 모델을 지원하는 제공자가 없습니다."
                ));
        return provider.analyze(input, candidateCount, model);
    }
}
