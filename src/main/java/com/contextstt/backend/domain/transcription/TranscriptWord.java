package com.contextstt.backend.domain.transcription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transcript_words")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TranscriptWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcription_id", nullable = false)
    private Transcription transcription;

    @Column(nullable = false)
    private int wordOrder;

    @Column(nullable = false, length = 2000)
    private String originalText;

    @Column(length = 2000)
    private String correctedText;

    private Long startOffsetMillis;

    private Long endOffsetMillis;

    private Float confidence;

    @Column(length = 50)
    private String speakerLabel;

    private LocalDateTime correctedAt;

    @Builder
    private TranscriptWord(
            Transcription transcription,
            int wordOrder,
            String originalText,
            Long startOffsetMillis,
            Long endOffsetMillis,
            Float confidence,
            String speakerLabel
    ) {
        this.transcription = transcription;
        this.wordOrder = wordOrder;
        this.originalText = originalText;
        this.startOffsetMillis = startOffsetMillis;
        this.endOffsetMillis = endOffsetMillis;
        this.confidence = confidence;
        this.speakerLabel = speakerLabel;
    }

    public String getCurrentText() {
        return correctedText == null ? originalText : correctedText;
    }

    void correct(String text) {
        correctedText = originalText.equals(text) ? null : text;
        correctedAt = correctedText == null ? null : LocalDateTime.now();
    }
}
