package com.contextstt.backend.domain.analysis;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "context_ambiguities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContextAmbiguity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private ContextAnalysis analysis;

    @Column(nullable = false)
    private int ambiguityOrder;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String excerpt;

    private Long startWordId;

    private Long endWordId;

    private Integer startWordOrder;

    private Integer endWordOrder;

    @Column(nullable = false)
    private int candidateCount;

    @OneToMany(mappedBy = "ambiguity", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("candidateRank ASC")
    private List<ContextCandidate> candidates = new ArrayList<>();

    @OneToOne(mappedBy = "ambiguity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ContextAnalysisSelection selection;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ContextAmbiguity(
            ContextAnalysis analysis,
            int ambiguityOrder,
            String excerpt,
            Long startWordId,
            Long endWordId,
            Integer startWordOrder,
            Integer endWordOrder
    ) {
        this.analysis = analysis;
        this.ambiguityOrder = ambiguityOrder;
        this.excerpt = excerpt;
        this.startWordId = startWordId;
        this.endWordId = endWordId;
        this.startWordOrder = startWordOrder;
        this.endWordOrder = endWordOrder;
    }

    public void addCandidate(
            int rank,
            String interpretation,
            String inferredIntent,
            String rationale,
            BigDecimal intentSimilarityScore
    ) {
        candidates.add(ContextCandidate.builder()
                .ambiguity(this)
                .candidateRank(rank)
                .interpretation(interpretation)
                .inferredIntent(inferredIntent)
                .rationale(rationale)
                .intentSimilarityScore(intentSimilarityScore)
                .build());
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
        analysis.touch();
        return selection;
    }

    public ContextAnalysisSelection editSelection(String text) {
        if (selection == null) {
            throw new ResourceConflictException("먼저 맥락 후보를 선택해 주세요.");
        }
        selection.edit(text);
        analysis.touch();
        return selection;
    }

    public ContextAnalysisSelection resolveCustom(String text) {
        if (selection == null) {
            selection = ContextAnalysisSelection.createCustom(this, text);
        } else {
            selection.changeToCustom(text);
        }
        analysis.touch();
        return selection;
    }

    public ContextAnalysisSelection dismiss() {
        if (selection == null) {
            selection = ContextAnalysisSelection.createDismissed(this);
        } else {
            selection.changeToDismissed();
        }
        analysis.touch();
        return selection;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
