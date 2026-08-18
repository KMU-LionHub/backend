package com.contextstt.backend.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ContextAnalysisResult(
        String provider,
        String model,
        List<GeneratedContextCandidate> candidates
) {

    public ContextAnalysisResult {
        candidates = candidates == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(candidates));
    }
}
