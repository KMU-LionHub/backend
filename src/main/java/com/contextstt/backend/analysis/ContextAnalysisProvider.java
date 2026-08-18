package com.contextstt.backend.analysis;

public interface ContextAnalysisProvider {

    boolean supports(ContextAnalysisModel model);

    ContextAnalysisResult analyze(
            ContextAnalysisInput input,
            int candidateCount,
            ContextAnalysisModel model
    );
}
