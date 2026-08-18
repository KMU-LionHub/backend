package com.contextstt.backend.domain.conversation;

import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.domain.transcription.TranscriptionStatus;
import com.contextstt.backend.exception.ResourceConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conversation_utterances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationUtterance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcription_id", nullable = false)
    private Transcription transcription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "speaker_participant_id", nullable = false)
    private ConversationParticipant speaker;

    @Column(nullable = false)
    private int utteranceOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ConversationUtterance(
            Conversation conversation,
            Transcription transcription,
            ConversationParticipant speaker,
            int utteranceOrder
    ) {
        this.conversation = conversation;
        this.transcription = transcription;
        this.speaker = speaker;
        this.utteranceOrder = utteranceOrder;
    }

    public void replaceTranscription(Transcription replacement) {
        conversation.ensureActive();
        if (transcription.getStatus() == TranscriptionStatus.CONFIRMED) {
            throw new ResourceConflictException("확정된 발언은 재발언으로 교체할 수 없습니다.");
        }
        if (!replacement.isReplacementOf(transcription.getId())) {
            throw new ResourceConflictException("현재 발언을 대체하기 위해 생성된 전사가 아닙니다.");
        }
        transcription.markSuperseded();
        transcription = replacement;
        updatedAt = LocalDateTime.now();
    }

    public void confirm() {
        conversation.ensureActive();
        transcription.confirm();
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
