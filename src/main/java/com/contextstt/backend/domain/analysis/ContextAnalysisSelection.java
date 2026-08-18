package com.contextstt.backend.domain.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "context_analysis_selections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContextAnalysisSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ambiguity_id", nullable = false, unique = true)
    private ContextAmbiguity ambiguity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private ContextCandidate candidate;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String originalCandidateText;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String finalText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime selectedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    static ContextAnalysisSelection create(ContextAmbiguity ambiguity, ContextCandidate candidate) {
        ContextAnalysisSelection selection = new ContextAnalysisSelection();
        selection.ambiguity = ambiguity;
        selection.applyCandidate(candidate);
        return selection;
    }

    void changeCandidate(ContextCandidate candidate) {
        applyCandidate(candidate);
        selectedAt = LocalDateTime.now();
        updatedAt = selectedAt;
    }

    void edit(String text) {
        finalText = text;
        updatedAt = LocalDateTime.now();
    }

    public boolean isEdited() {
        return !originalCandidateText.equals(finalText);
    }

    private void applyCandidate(ContextCandidate candidate) {
        this.candidate = candidate;
        originalCandidateText = candidate.getInterpretation();
        finalText = candidate.getInterpretation();
    }

    @PrePersist
    protected void onCreate() {
        selectedAt = LocalDateTime.now();
        updatedAt = selectedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
