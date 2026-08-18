package com.contextstt.backend.domain.analysis;

import com.contextstt.backend.domain.conversation.Conversation;
import com.contextstt.backend.domain.conversation.ConversationUtterance;
import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.exception.ContextAmbiguityNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "context_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContextAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utterance_id", nullable = false)
    private ConversationUtterance utterance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcription_id", nullable = false)
    private Transcription transcription;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(length = 50)
    private String sourceSpeakerName;

    @Column(columnDefinition = "LONGTEXT")
    private String conversationContext;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String sourceOriginalText;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String sourceCurrentText;

    @Column(nullable = false)
    private int requestedCandidateCount;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ambiguityOrder ASC")
    private List<ContextAmbiguity> ambiguities = new ArrayList<>();

    @Column(nullable = false)
    private int ambiguityCount;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ContextAnalysis(
            Conversation conversation,
            ConversationUtterance utterance,
            Transcription transcription,
            String provider,
            String model,
            String sourceSpeakerName,
            String conversationContext,
            String sourceOriginalText,
            String sourceCurrentText,
            int requestedCandidateCount
    ) {
        this.conversation = conversation;
        this.utterance = utterance;
        this.transcription = transcription;
        this.provider = provider;
        this.model = model;
        this.sourceSpeakerName = sourceSpeakerName;
        this.conversationContext = conversationContext;
        this.sourceOriginalText = sourceOriginalText;
        this.sourceCurrentText = sourceCurrentText;
        this.requestedCandidateCount = requestedCandidateCount;
    }

    public ContextAmbiguity addAmbiguity(
            int order,
            String excerpt,
            Long startWordId,
            Long endWordId,
            Integer startWordOrder,
            Integer endWordOrder
    ) {
        ContextAmbiguity ambiguity = ContextAmbiguity.builder()
                .analysis(this)
                .ambiguityOrder(order)
                .excerpt(excerpt)
                .startWordId(startWordId)
                .endWordId(endWordId)
                .startWordOrder(startWordOrder)
                .endWordOrder(endWordOrder)
                .build();
        ambiguities.add(ambiguity);
        ambiguityCount = ambiguities.size();
        return ambiguity;
    }

    public ContextAmbiguity findAmbiguity(Long ambiguityId) {
        return ambiguities.stream()
                .filter(item -> item.getId().equals(ambiguityId))
                .findFirst()
                .orElseThrow(ContextAmbiguityNotFoundException::new);
    }

    public int getResolvedAmbiguityCount() {
        return (int) ambiguities.stream()
                .filter(ambiguity -> ambiguity.getSelection() != null)
                .count();
    }

    public boolean isFullyResolved() {
        return ambiguities.stream().allMatch(ambiguity -> ambiguity.getSelection() != null);
    }

    public boolean isStale() {
        Transcription currentTranscription = utterance.getTranscription();
        return !currentTranscription.getId().equals(transcription.getId())
                || !currentTranscription.getCurrentText().equals(sourceCurrentText);
    }

    public boolean hasUsableResolution() {
        return !isStale() && isFullyResolved();
    }

    void touch() {
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
