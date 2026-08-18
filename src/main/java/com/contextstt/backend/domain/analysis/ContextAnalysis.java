package com.contextstt.backend.domain.analysis;

import com.contextstt.backend.domain.conversation.Conversation;
import com.contextstt.backend.domain.conversation.ConversationUtterance;
import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.exception.ContextCandidateNotFoundException;
import com.contextstt.backend.exception.ResourceConflictException;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
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
    private int candidateCount;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("candidateRank ASC")
    private List<ContextCandidate> candidates = new ArrayList<>();

    @OneToOne(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ContextAnalysisSelection selection;

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
            String sourceCurrentText
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
    }

    public void addCandidate(
            int rank,
            String interpretation,
            String inferredIntent,
            String rationale,
            BigDecimal intentSimilarityScore
    ) {
        ContextCandidate candidate = ContextCandidate.builder()
                .analysis(this)
                .candidateRank(rank)
                .interpretation(interpretation)
                .inferredIntent(inferredIntent)
                .rationale(rationale)
                .intentSimilarityScore(intentSimilarityScore)
                .build();
        candidates.add(candidate);
        candidateCount = candidates.size();
    }

    public ContextAnalysisSelection selectCandidate(Long candidateId) {
        ContextCandidate candidate = candidates.stream()
                .filter(item -> item.getId().equals(candidateId))
                .findFirst()
                .orElseThrow(ContextCandidateNotFoundException::new);

        if (selection == null) {
            selection = ContextAnalysisSelection.create(this, candidate);
        } else {
            selection.changeCandidate(candidate);
        }
        touch();
        return selection;
    }

    public ContextAnalysisSelection editSelection(String text) {
        if (selection == null) {
            throw new ResourceConflictException("먼저 맥락 후보를 선택해 주세요.");
        }
        selection.edit(text);
        touch();
        return selection;
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
