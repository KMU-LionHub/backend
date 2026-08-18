package com.contextstt.backend.analysis;

import java.util.List;

public record ContextCandidateGroup(String excerpt, List<ContextCandidateOption> candidates) {
}
