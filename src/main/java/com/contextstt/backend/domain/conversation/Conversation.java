package com.contextstt.backend.domain.conversation;

import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.domain.transcription.TranscriptionStatus;
import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.exception.ConversationParticipantNotFoundException;
import com.contextstt.backend.exception.ConversationUtteranceNotFoundException;
import com.contextstt.backend.exception.ResourceConflictException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conversation_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String context;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(nullable = false)
    private int nextUtteranceOrder;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ConversationParticipant> participants = new LinkedHashSet<>();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("utteranceOrder ASC")
    private List<ConversationUtterance> utterances = new ArrayList<>();

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Conversation(User owner, String title, String context) {
        this.owner = owner;
        this.title = title;
        this.context = context;
    }

    public ConversationParticipant addSelfParticipant() {
        ConversationParticipant participant = ConversationParticipant.builder()
                .conversation(this)
                .user(owner)
                .displayName(owner.getNickname())
                .type(ParticipantType.SELF)
                .build();
        participants.add(participant);
        return participant;
    }

    public ConversationParticipant addOtherParticipant(String displayName) {
        ensureActive();
        String normalizedName = displayName.trim();
        boolean duplicateName = participants.stream()
                .anyMatch(participant -> participant.getDisplayName().equalsIgnoreCase(normalizedName));
        if (duplicateName) {
            throw new ResourceConflictException("같은 이름의 대화 참여자가 이미 존재합니다.");
        }
        ConversationParticipant participant = ConversationParticipant.builder()
                .conversation(this)
                .displayName(normalizedName)
                .type(ParticipantType.OTHER)
                .build();
        participants.add(participant);
        touch();
        return participant;
    }

    public ConversationUtterance addUtterance(
            Transcription transcription,
            ConversationParticipant speaker
    ) {
        ensureActive();
        transcription.ensureAttachable();
        ConversationUtterance utterance = ConversationUtterance.builder()
                .conversation(this)
                .transcription(transcription)
                .speaker(speaker)
                .utteranceOrder(nextUtteranceOrder++)
                .build();
        utterances.add(utterance);
        touch();
        return utterance;
    }

    public ConversationUtterance replaceUtteranceTranscription(
            Long utteranceId,
            Transcription replacement
    ) {
        ConversationUtterance utterance = findUtterance(utteranceId);
        utterance.replaceTranscription(replacement);
        touch();
        return utterance;
    }

    public ConversationUtterance confirmUtterance(Long utteranceId) {
        ConversationUtterance utterance = findUtterance(utteranceId);
        utterance.confirm();
        touch();
        return utterance;
    }

    public ConversationParticipant findParticipant(Long participantId) {
        return participants.stream()
                .filter(participant -> participant.getId().equals(participantId))
                .findFirst()
                .orElseThrow(ConversationParticipantNotFoundException::new);
    }

    public ConversationUtterance findUtterance(Long utteranceId) {
        return utterances.stream()
                .filter(utterance -> utterance.getId().equals(utteranceId))
                .findFirst()
                .orElseThrow(ConversationUtteranceNotFoundException::new);
    }

    public List<ConversationParticipant> getParticipantsInOrder() {
        return participants.stream()
                .sorted(Comparator.comparing(ConversationParticipant::getId))
                .toList();
    }

    public int getUtteranceCount() {
        return nextUtteranceOrder;
    }

    public void close() {
        if (status == ConversationStatus.CLOSED) {
            return;
        }
        if (utterances.isEmpty()) {
            throw new ResourceConflictException("발언이 없는 대화는 종료할 수 없습니다.");
        }
        boolean hasUnconfirmedUtterance = utterances.stream()
                .anyMatch(utterance -> utterance.getTranscription().getStatus() != TranscriptionStatus.CONFIRMED);
        if (hasUnconfirmedUtterance) {
            throw new ResourceConflictException("확정되지 않은 발언이 있어 대화를 종료할 수 없습니다.");
        }
        status = ConversationStatus.CLOSED;
        touch();
    }

    public void ensureActive() {
        if (status == ConversationStatus.CLOSED) {
            throw new ResourceConflictException("종료된 대화는 변경할 수 없습니다.");
        }
    }

    private void touch() {
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
