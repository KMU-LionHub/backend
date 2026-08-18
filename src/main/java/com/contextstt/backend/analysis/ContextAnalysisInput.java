package com.contextstt.backend.analysis;

import java.util.List;

public record ContextAnalysisInput(
        Long conversationId,
        Long utteranceId,
        String conversationContext,
        List<AnalysisParticipant> participants,
        List<PreviousUtterance> previousUtterances,
        String targetSpeakerName,
        String targetOriginalText,
        String targetCurrentText
) {

    public ContextAnalysisInput {
        participants = List.copyOf(participants);
        previousUtterances = List.copyOf(previousUtterances);
    }

    public record AnalysisParticipant(
            Long id,
            String displayName,
            boolean self
    ) {
    }

    public record PreviousUtterance(
            int order,
            String speakerName,
            String text
    ) {
    }
}
