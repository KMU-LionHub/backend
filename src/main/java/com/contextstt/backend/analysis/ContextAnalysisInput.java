package com.contextstt.backend.analysis;

import java.util.List;

public record ContextAnalysisInput(
        Long conversationId,
        Long utteranceId,
        String conversationContext,
        List<AnalysisParticipant> participants,
        List<PreviousUtterance> previousUtterances,
        int omittedPreviousUtteranceCount,
        String targetSpeakerName,
        String targetOriginalText,
        String targetCurrentText,
        List<AnalysisWord> targetWords
) {

    public ContextAnalysisInput {
        participants = List.copyOf(participants);
        previousUtterances = List.copyOf(previousUtterances);
        targetWords = List.copyOf(targetWords);
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

    public record AnalysisWord(
            int order,
            String text
    ) {
    }
}
