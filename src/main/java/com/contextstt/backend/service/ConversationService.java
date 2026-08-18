package com.contextstt.backend.service;

import com.contextstt.backend.domain.conversation.Conversation;
import com.contextstt.backend.domain.conversation.ConversationParticipant;
import com.contextstt.backend.domain.conversation.ConversationRepository;
import com.contextstt.backend.domain.conversation.ConversationUtterance;
import com.contextstt.backend.domain.conversation.ConversationUtteranceRepository;
import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.domain.transcription.TranscriptionRepository;
import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.domain.user.UserRepository;
import com.contextstt.backend.dto.conversation.AddUtteranceRequest;
import com.contextstt.backend.dto.conversation.ConversationPageResponse;
import com.contextstt.backend.dto.conversation.ConversationParticipantResponse;
import com.contextstt.backend.dto.conversation.ConversationResponse;
import com.contextstt.backend.dto.conversation.ConversationSummaryResponse;
import com.contextstt.backend.dto.conversation.ConversationUtteranceResponse;
import com.contextstt.backend.dto.conversation.CreateConversationRequest;
import com.contextstt.backend.dto.conversation.CreateParticipantRequest;
import com.contextstt.backend.dto.conversation.ReplaceUtteranceTranscriptionRequest;
import com.contextstt.backend.exception.ConversationNotFoundException;
import com.contextstt.backend.exception.InvalidRequestException;
import com.contextstt.backend.exception.ResourceConflictException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ConversationRepository conversationRepository;
    private final ConversationUtteranceRepository utteranceRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConversationResponse create(Long ownerId, CreateConversationRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Conversation conversation = Conversation.builder()
                .owner(owner)
                .title(request.title().trim())
                .context(normalizeOptionalText(request.context()))
                .build();
        conversation.addSelfParticipant();
        participantsOrEmpty(request.participants()).stream()
                .map(CreateParticipantRequest::displayName)
                .forEach(conversation::addOtherParticipant);

        return ConversationResponse.from(conversationRepository.saveAndFlush(conversation));
    }

    @Transactional(readOnly = true)
    public ConversationPageResponse list(Long ownerId, int page, int size) {
        validatePage(page, size);
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
        );
        Page<ConversationSummaryResponse> conversations = conversationRepository
                .findByOwnerId(ownerId, pageRequest)
                .map(ConversationSummaryResponse::from);
        return ConversationPageResponse.from(conversations);
    }

    @Transactional(readOnly = true)
    public ConversationResponse get(Long ownerId, Long conversationId) {
        return ConversationResponse.from(findOwnedConversation(conversationId, ownerId));
    }

    @Transactional
    public ConversationParticipantResponse addParticipant(
            Long ownerId,
            Long conversationId,
            CreateParticipantRequest request
    ) {
        Conversation conversation = findOwnedConversation(conversationId, ownerId);
        ConversationParticipant participant = conversation.addOtherParticipant(request.displayName());
        conversationRepository.flush();
        return ConversationParticipantResponse.from(participant);
    }

    @Transactional
    public ConversationUtteranceResponse addUtterance(
            Long ownerId,
            Long conversationId,
            AddUtteranceRequest request
    ) {
        Conversation conversation = findOwnedConversation(conversationId, ownerId);
        ConversationParticipant speaker = conversation.findParticipant(request.speakerParticipantId());
        Transcription transcription = findOwnedTranscription(request.transcriptionId(), ownerId);
        ensureTranscriptionAvailable(transcription.getId());

        ConversationUtterance utterance = conversation.addUtterance(transcription, speaker);
        flushConversationWithConflictMapping();
        return ConversationUtteranceResponse.from(utterance);
    }

    @Transactional
    public ConversationUtteranceResponse replaceUtteranceTranscription(
            Long ownerId,
            Long conversationId,
            Long utteranceId,
            ReplaceUtteranceTranscriptionRequest request
    ) {
        Conversation conversation = findOwnedConversation(conversationId, ownerId);
        Transcription replacement = findOwnedTranscription(request.transcriptionId(), ownerId);
        ensureTranscriptionAvailable(replacement.getId());

        ConversationUtterance utterance = conversation.replaceUtteranceTranscription(
                utteranceId,
                replacement
        );
        flushConversationWithConflictMapping();
        return ConversationUtteranceResponse.from(utterance);
    }

    @Transactional
    public ConversationUtteranceResponse confirmUtterance(
            Long ownerId,
            Long conversationId,
            Long utteranceId
    ) {
        Conversation conversation = findOwnedConversation(conversationId, ownerId);
        ConversationUtterance utterance = conversation.confirmUtterance(utteranceId);
        conversationRepository.flush();
        return ConversationUtteranceResponse.from(utterance);
    }

    @Transactional
    public ConversationResponse close(Long ownerId, Long conversationId) {
        Conversation conversation = findOwnedConversation(conversationId, ownerId);
        conversation.close();
        conversationRepository.flush();
        return ConversationResponse.from(conversation);
    }

    private Conversation findOwnedConversation(Long conversationId, Long ownerId) {
        Conversation conversation = conversationRepository
                .findWithParticipantsByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(ConversationNotFoundException::new);
        conversationRepository.findWithUtterancesByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(ConversationNotFoundException::new);
        return conversation;
    }

    private Transcription findOwnedTranscription(Long transcriptionId, Long ownerId) {
        return transcriptionRepository.findByIdAndUserId(transcriptionId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("전사 기록을 찾을 수 없습니다."));
    }

    private void ensureTranscriptionAvailable(Long transcriptionId) {
        if (utteranceRepository.existsByTranscriptionId(transcriptionId)) {
            throw new ResourceConflictException("이미 다른 대화 발언에 연결된 전사 기록입니다.");
        }
    }

    private void flushConversationWithConflictMapping() {
        try {
            conversationRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("대화 발언을 동시에 변경했습니다. 다시 시도해 주세요.");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }
    }

    private String normalizeOptionalText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private List<CreateParticipantRequest> participantsOrEmpty(List<CreateParticipantRequest> participants) {
        return participants == null ? List.of() : participants;
    }
}
