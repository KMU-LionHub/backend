package com.contextstt.backend.domain.transcription;

import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.exception.TranscriptWordNotFoundException;
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
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transcriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transcription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaces_transcription_id")
    private Transcription replacesTranscription;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false, length = 35)
    private String languageCode;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String originalText;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String currentText;

    private Float confidence;

    @Column(length = 100)
    private String audioContentType;

    @Column(nullable = false)
    private long audioSizeBytes;

    @OneToMany(mappedBy = "transcription", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("wordOrder ASC")
    private List<TranscriptWord> words = new ArrayList<>();

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Transcription(
            User user,
            Transcription replacesTranscription,
            String provider,
            String model,
            String languageCode,
            String originalText,
            Float confidence,
            String audioContentType,
            long audioSizeBytes
    ) {
        this.user = user;
        this.replacesTranscription = replacesTranscription;
        this.provider = provider;
        this.model = model;
        this.languageCode = languageCode;
        this.originalText = originalText;
        this.currentText = originalText;
        this.confidence = confidence;
        this.audioContentType = audioContentType;
        this.audioSizeBytes = audioSizeBytes;
    }

    public void addWord(
            String text,
            Long startOffsetMillis,
            Long endOffsetMillis,
            Float wordConfidence,
            String speakerLabel
    ) {
        words.add(TranscriptWord.builder()
                .transcription(this)
                .wordOrder(words.size())
                .originalText(text)
                .startOffsetMillis(startOffsetMillis)
                .endOffsetMillis(endOffsetMillis)
                .confidence(wordConfidence)
                .speakerLabel(speakerLabel)
                .build());
    }

    public void correctWord(Long wordId, String text) {
        TranscriptWord word = words.stream()
                .filter(candidate -> candidate.getId().equals(wordId))
                .findFirst()
                .orElseThrow(TranscriptWordNotFoundException::new);

        word.correct(text);
        currentText = words.stream()
                .map(TranscriptWord::getCurrentText)
                .collect(Collectors.joining(" "));
    }

    public Optional<Long> getReplacesTranscriptionId() {
        return replacesTranscription == null
                ? Optional.empty()
                : Optional.of(replacesTranscription.getId());
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
