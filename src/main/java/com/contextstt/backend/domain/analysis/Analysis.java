package com.contextstt.backend.domain.analysis;

import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.domain.user.User;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcription_id", nullable = false)
    private Transcription transcription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Analysis(User user, Transcription transcription, String provider, String model) {
        this.user = user;
        this.transcription = transcription;
        this.provider = provider;
        this.model = model;
        this.status = AnalysisStatus.PENDING;
        this.progress = 0;
    }

    public void markInProgress(int progress) {
        this.status = AnalysisStatus.IN_PROGRESS;
        this.progress = progress;
    }

    public void markCompleted(String resultJson) {
        this.status = AnalysisStatus.COMPLETED;
        this.progress = 100;
        this.resultJson = resultJson;
    }

    public void markFailed(String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
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
