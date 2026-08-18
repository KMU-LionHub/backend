package com.contextstt.backend.domain.transcription;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {

    @EntityGraph(attributePaths = {"words", "replacesTranscription"})
    Optional<Transcription> findByIdAndUserId(Long id, Long userId);
}
