package com.contextstt.backend.domain.analysis;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByIdAndUserId(Long id, Long userId);
}
