package com.contextstt.backend.analysis;

import com.contextstt.backend.analysis.ContextAnalysisInput.AnalysisParticipant;
import com.contextstt.backend.analysis.ContextAnalysisInput.AnalysisWord;
import com.contextstt.backend.analysis.ContextAnalysisInput.PreviousUtterance;
import com.contextstt.backend.domain.conversation.Conversation;
import com.contextstt.backend.domain.conversation.ConversationRepository;
import com.contextstt.backend.domain.conversation.ConversationUtterance;
import com.contextstt.backend.domain.conversation.ParticipantType;
import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.domain.transcription.TranscriptionStatus;
import com.contextstt.backend.exception.ConversationNotFoundException;
import com.contextstt.backend.exception.ResourceConflictException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContextAnalysisSourceLoader {

    private final ConversationRepository conversationRepository;

    @Transactional(readOnly = true)
    public ContextAnalysisSource load(Long ownerId, Long conversationId, Long utteranceId) {
        Conversation conversation = conversationRepository
                .findWithParticipantsByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(ConversationNotFoundException::new);
        conversationRepository.findWithUtterancesByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(ConversationNotFoundException::new);

        ConversationUtterance target = conversation.findUtterance(utteranceId);
        Transcription transcription = target.getTranscription();
        if (transcription.getStatus() == TranscriptionStatus.SUPERSEDED) {
            throw new ResourceConflictException("재발언으로 대체된 발언은 맥락을 분석할 수 없습니다.");
        }

        List<AnalysisParticipant> participants = conversation.getParticipantsInOrder().stream()
                .map(participant -> new AnalysisParticipant(
                        participant.getId(),
                        participant.getDisplayName(),
                        participant.getType() == ParticipantType.SELF
                ))
                .toList();

        List<PreviousUtterance> previousUtterances = conversation.getUtterances().stream()
                .filter(utterance -> utterance.getUtteranceOrder() < target.getUtteranceOrder())
                .filter(utterance -> utterance.getTranscription().getStatus() == TranscriptionStatus.CONFIRMED)
                .map(utterance -> new PreviousUtterance(
                        utterance.getUtteranceOrder(),
                        utterance.getSpeaker().getDisplayName(),
                        utterance.getTranscription().getCurrentText()
                ))
                .toList();

        ContextAnalysisInput input = new ContextAnalysisInput(
                conversation.getId(),
                target.getId(),
                conversation.getContext(),
                participants,
                previousUtterances,
                target.getSpeaker().getDisplayName(),
                transcription.getOriginalText(),
                transcription.getCurrentText(),
                transcription.getWords().stream()
                        .map(word -> new AnalysisWord(word.getWordOrder(), word.getCurrentText()))
                        .toList()
        );
        return new ContextAnalysisSource(conversation, target, transcription, input);
    }
}
