package com.contextstt.backend.domain.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "context_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContextCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ambiguity_id", nullable = false)
    private ContextAmbiguity ambiguity;

    @Column(nullable = false)
    private int candidateRank;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String interpretation;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String inferredIntent;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String rationale;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal intentSimilarityScore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ContextCandidate(
            ContextAmbiguity ambiguity,
            int candidateRank,
            String interpretation,
            String inferredIntent,
            String rationale,
            BigDecimal intentSimilarityScore
    ) {
        this.ambiguity = ambiguity;
        this.candidateRank = candidateRank;
        this.interpretation = interpretation;
        this.inferredIntent = inferredIntent;
        this.rationale = rationale;
        this.intentSimilarityScore = intentSimilarityScore;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
