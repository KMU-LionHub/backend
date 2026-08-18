package com.contextstt.backend.domain.analysis;

import com.contextstt.backend.exception.ResourceConflictException;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private ContextCandidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContextResolutionType resolutionType;

    @Column(columnDefinition = "LONGTEXT")
    private String originalCandidateText;

    @Column(columnDefinition = "LONGTEXT")
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

    static ContextAnalysisSelection createCustom(ContextAmbiguity ambiguity, String text) {
        ContextAnalysisSelection selection = new ContextAnalysisSelection();
        selection.ambiguity = ambiguity;
        selection.applyCustom(text);
        return selection;
    }

    static ContextAnalysisSelection createDismissed(ContextAmbiguity ambiguity) {
        ContextAnalysisSelection selection = new ContextAnalysisSelection();
        selection.ambiguity = ambiguity;
        selection.applyDismissed();
        return selection;
    }

    void changeCandidate(ContextCandidate candidate) {
        applyCandidate(candidate);
        selectedAt = LocalDateTime.now();
        updatedAt = selectedAt;
    }

    void edit(String text) {
        if (resolutionType == ContextResolutionType.DISMISSED) {
            throw new ResourceConflictException(
                    "무시 처리한 구간은 문구를 수정할 수 없습니다."
            );
        }
        finalText = text;
        updatedAt = LocalDateTime.now();
    }

    void changeToCustom(String text) {
        applyCustom(text);
        selectedAt = LocalDateTime.now();
        updatedAt = selectedAt;
    }

    void changeToDismissed() {
        applyDismissed();
        selectedAt = LocalDateTime.now();
        updatedAt = selectedAt;
    }

    public boolean isEdited() {
        return resolutionType == ContextResolutionType.CANDIDATE
                && !originalCandidateText.equals(finalText);
    }

    private void applyCandidate(ContextCandidate candidate) {
        this.candidate = candidate;
        resolutionType = ContextResolutionType.CANDIDATE;
        originalCandidateText = candidate.getInterpretation();
        finalText = candidate.getInterpretation();
    }

    private void applyCustom(String text) {
        candidate = null;
        resolutionType = ContextResolutionType.CUSTOM;
        originalCandidateText = null;
        finalText = text;
    }

    private void applyDismissed() {
        candidate = null;
        resolutionType = ContextResolutionType.DISMISSED;
        originalCandidateText = null;
        finalText = null;
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
