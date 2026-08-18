package com.contextstt.backend.analysis;

public interface ContextAnalysisGateway {

    ContextAnalysisResult analyze(
            ContextAnalysisInput input,
            int candidateCount,
            ContextAnalysisModel model
    );
}
