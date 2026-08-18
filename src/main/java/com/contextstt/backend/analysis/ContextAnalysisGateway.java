package com.contextstt.backend.analysis;

public interface ContextAnalysisGateway {

    ContextAnalysisResult analyze(String transcriptText);

    String provider();

    String model();
}
