package com.contextstt.backend.domain.analysis;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContextAnalysisRepository extends JpaRepository<ContextAnalysis, Long> {

    @EntityGraph(attributePaths = {"candidates", "selection", "selection.candidate"})
    @Query("select distinct analysis from ContextAnalysis analysis "
            + "where analysis.id = :analysisId and analysis.conversation.owner.id = :ownerId")
    Optional<ContextAnalysis> findDetailedByIdAndOwnerId(
            @Param("analysisId") Long analysisId,
            @Param("ownerId") Long ownerId
    );

    @EntityGraph(attributePaths = {"selection", "selection.candidate"})
    List<ContextAnalysis> findByConversationIdAndUtteranceIdAndConversationOwnerIdOrderByCreatedAtDesc(
            Long conversationId,
            Long utteranceId,
            Long ownerId
    );
}
