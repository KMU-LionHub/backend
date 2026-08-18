package com.contextstt.backend.domain.conversation;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"participants", "participants.user"})
    @Query("select distinct conversation from Conversation conversation "
            + "where conversation.id = :conversationId and conversation.owner.id = :ownerId")
    Optional<Conversation> findWithParticipantsByIdAndOwnerId(
            @Param("conversationId") Long conversationId,
            @Param("ownerId") Long ownerId
    );

    @EntityGraph(attributePaths = {
            "utterances",
            "utterances.speaker",
            "utterances.transcription"
    })
    @Query("select distinct conversation from Conversation conversation "
            + "where conversation.id = :conversationId and conversation.owner.id = :ownerId")
    Optional<Conversation> findWithUtterancesByIdAndOwnerId(
            @Param("conversationId") Long conversationId,
            @Param("ownerId") Long ownerId
    );
}
