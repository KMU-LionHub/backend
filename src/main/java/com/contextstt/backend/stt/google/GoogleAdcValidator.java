package com.contextstt.backend.stt.google;

import com.contextstt.backend.exception.SpeechProviderUnavailableException;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class GoogleAdcValidator {

    private final CredentialsLoader credentialsLoader;

    public GoogleAdcValidator() {
        this(GoogleCredentials::getApplicationDefault);
    }

    GoogleAdcValidator(CredentialsLoader credentialsLoader) {
        this.credentialsLoader = credentialsLoader;
    }

    public void validate() {
        try {
            credentialsLoader.load();
        } catch (IOException ex) {
            throw new SpeechProviderUnavailableException(
                    "Google ADC 인증 정보를 찾을 수 없습니다. "
                            + "로컬에서는 gcloud auth application-default login을 실행하고, "
                            + "Docker에서는 ADC 파일을 마운트해 주세요.",
                    ex
            );
        }
    }

    @FunctionalInterface
    interface CredentialsLoader {
        GoogleCredentials load() throws IOException;
    }
}
