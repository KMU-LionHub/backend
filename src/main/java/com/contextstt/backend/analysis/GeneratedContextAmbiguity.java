package com.contextstt.backend.analysis;

import java.util.List;

public record GeneratedContextAmbiguity(
        Integer startWordOrder,
        Integer endWordOrder,
        List<GeneratedContextCandidate> candidates
) {
}
