package com.contextstt.backend.service;

import com.contextstt.backend.domain.transcription.Transcription;
import com.contextstt.backend.domain.transcription.TranscriptionRepository;
import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.domain.user.UserRepository;
import com.contextstt.backend.dto.stt.TranscriptionResponse;
import com.contextstt.backend.exception.AudioTooLargeException;
import com.contextstt.backend.exception.InvalidAudioException;
import com.contextstt.backend.exception.TranscriptionNotFoundException;
import com.contextstt.backend.stt.RecognizedWord;
import com.contextstt.backend.stt.SpeechRecognitionResult;
import com.contextstt.backend.stt.SpeechToTextGateway;
import com.contextstt.backend.stt.google.GoogleSttProperties;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private static final Pattern LANGUAGE_CODE_PATTERN =
            Pattern.compile("^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$");
    private static final Set<String> SUPPORTED_VIDEO_TYPES =
            Set.of("video/webm", "video/mp4", "video/quicktime");
    private static final String BINARY_CONTENT_TYPE = "application/octet-stream";

    private final TranscriptionRepository transcriptionRepository;
    private final UserRepository userRepository;
    private final SpeechToTextGateway speechToTextGateway;
    private final GoogleSttProperties sttProperties;

    public TranscriptionResponse transcribe(Long userId, MultipartFile audio, String languageCode) {
        return createTranscription(userId, null, audio, languageCode);
    }

    public TranscriptionResponse reRecord(
            Long userId,
            Long transcriptionId,
            MultipartFile audio,
            String languageCode
    ) {
        Transcription previous = findOwnedTranscription(transcriptionId, userId);
        previous.ensureRerecordable();
        return createTranscription(userId, previous, audio, languageCode);
    }

    @Transactional(readOnly = true)
    public TranscriptionResponse get(Long userId, Long transcriptionId) {
        return TranscriptionResponse.from(findOwnedTranscription(transcriptionId, userId));
    }

    @Transactional
    public TranscriptionResponse correctWord(
            Long userId,
            Long transcriptionId,
            Long wordId,
            String text
    ) {
        Transcription transcription = findOwnedTranscription(transcriptionId, userId);
        transcription.correctWord(wordId, text.trim());
        transcriptionRepository.flush();
        return TranscriptionResponse.from(transcription);
    }

    private TranscriptionResponse createTranscription(
            Long userId,
            Transcription replacesTranscription,
            MultipartFile audio,
            String requestedLanguageCode
    ) {
        byte[] audioContent = validateAndRead(audio);
        String languageCode = normalizeLanguageCode(requestedLanguageCode);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("사용자를 찾을 수 없습니다."));

        SpeechRecognitionResult recognition = speechToTextGateway.recognize(audioContent, languageCode);
        Transcription transcription = Transcription.builder()
                .user(user)
                .replacesTranscription(replacesTranscription)
                .provider(speechToTextGateway.provider())
                .model(speechToTextGateway.model())
                .languageCode(languageCode)
                .originalText(recognition.transcript())
                .confidence(recognition.confidence())
                .audioContentType(normalizeContentType(audio.getContentType()))
                .audioSizeBytes(audioContent.length)
                .build();

        editableWords(recognition).forEach(word -> transcription.addWord(
                word.text(),
                word.startOffsetMillis(),
                word.endOffsetMillis(),
                word.confidence(),
                word.speakerLabel()
        ));

        return TranscriptionResponse.from(transcriptionRepository.saveAndFlush(transcription));
    }

    private byte[] validateAndRead(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new InvalidAudioException("오디오 파일은 필수입니다.");
        }
        if (audio.getSize() > sttProperties.getMaxAudioBytes()) {
            throw new AudioTooLargeException(sttProperties.getMaxAudioBytes());
        }

        String contentType = normalizeContentType(audio.getContentType());
        if (contentType != null
                && !contentType.startsWith("audio/")
                && !SUPPORTED_VIDEO_TYPES.contains(contentType)
                && !BINARY_CONTENT_TYPE.equals(contentType)) {
            throw new InvalidAudioException("지원하지 않는 오디오 미디어 타입입니다: " + contentType);
        }

        try {
            return audio.getBytes();
        } catch (IOException ex) {
            throw new InvalidAudioException("오디오 파일을 읽을 수 없습니다.", ex);
        }
    }

    private String normalizeLanguageCode(String requestedLanguageCode) {
        String candidate = StringUtils.hasText(requestedLanguageCode)
                ? requestedLanguageCode.trim()
                : "ko-KR";
        if (candidate.length() > 35 || !LANGUAGE_CODE_PATTERN.matcher(candidate).matches()) {
            throw new InvalidAudioException("언어 코드는 BCP-47 형식이어야 합니다.");
        }

        String normalized = Locale.forLanguageTag(candidate).toLanguageTag();
        if ("und".equals(normalized)) {
            throw new InvalidAudioException("지원할 수 없는 언어 코드입니다.");
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100);
    }

    private List<RecognizedWord> editableWords(SpeechRecognitionResult recognition) {
        if (!recognition.words().isEmpty()) {
            return recognition.words();
        }
        return Arrays.stream(recognition.transcript().trim().split("\\s+"))
                .filter(StringUtils::hasText)
                .map(text -> new RecognizedWord(text, null, null, null, null))
                .toList();
    }

    private Transcription findOwnedTranscription(Long transcriptionId, Long userId) {
        return transcriptionRepository.findByIdAndUserId(transcriptionId, userId)
                .orElseThrow(TranscriptionNotFoundException::new);
    }
}
