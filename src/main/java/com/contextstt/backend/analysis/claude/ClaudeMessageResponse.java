package com.contextstt.backend.analysis.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record ClaudeMessageResponse(String id, List<ClaudeContentBlock> content) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClaudeContentBlock(String type, String text) {
    }
}
