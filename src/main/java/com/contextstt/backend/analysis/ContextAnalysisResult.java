package com.contextstt.backend.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ContextAnalysisResult(
        String provider,
        String model,
        List<GeneratedContextAmbiguity> ambiguities
) {

    public ContextAnalysisResult {
        ambiguities = ambiguities == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(ambiguities));
    }
}
