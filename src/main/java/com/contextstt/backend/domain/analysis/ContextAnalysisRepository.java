package com.contextstt.backend.domain.analysis;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContextAnalysisRepository extends JpaRepository<ContextAnalysis, Long> {

    @EntityGraph(attributePaths = {
            "ambiguities",
            "ambiguities.selection",
            "ambiguities.selection.candidate"
    })
    @Query("select distinct analysis from ContextAnalysis analysis "
            + "where analysis.id = :analysisId and analysis.conversation.owner.id = :ownerId")
    Optional<ContextAnalysis> findDetailedByIdAndOwnerId(
            @Param("analysisId") Long analysisId,
            @Param("ownerId") Long ownerId
    );

    @EntityGraph(attributePaths = {
            "ambiguities",
            "ambiguities.selection",
            "ambiguities.selection.candidate"
    })
    List<ContextAnalysis> findByConversationIdAndUtteranceIdAndConversationOwnerIdOrderByCreatedAtDesc(
            Long conversationId,
            Long utteranceId,
            Long ownerId
    );

    @Query("select analysis.id from ContextAnalysis analysis "
            + "where analysis.conversation.id = :conversationId "
            + "and analysis.utterance.id = :utteranceId "
            + "and analysis.conversation.owner.id = :ownerId "
            + "and analysis.transcription.id = analysis.utterance.transcription.id "
            + "and analysis.sourceCurrentText = analysis.utterance.transcription.currentText "
            + "and not exists ("
            + "select ambiguity.id from ContextAmbiguity ambiguity "
            + "where ambiguity.analysis = analysis and ambiguity.selection is null"
            + ") order by analysis.createdAt desc, analysis.id desc")
    List<Long> findUsableAnalysisIds(
            @Param("conversationId") Long conversationId,
            @Param("utteranceId") Long utteranceId,
            @Param("ownerId") Long ownerId,
            Pageable pageable
    );
}
