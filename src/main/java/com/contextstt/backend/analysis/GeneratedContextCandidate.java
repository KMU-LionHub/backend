package com.contextstt.backend.analysis;

import java.math.BigDecimal;

public record GeneratedContextCandidate(
        String interpretation,
        String inferredIntent,
        String rationale,
        BigDecimal intentSimilarityScore
) {
}
