package com.contextstt.backend.stt.google;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contextstt.backend.exception.SpeechProviderUnavailableException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class GoogleAdcValidatorTest {

    @Test
    void reportsActionableErrorWhenApplicationDefaultCredentialsAreMissing() {
        GoogleAdcValidator validator = new GoogleAdcValidator(() -> {
            throw new IOException("ADC unavailable");
        });

        assertThatThrownBy(validator::validate)
                .isInstanceOf(SpeechProviderUnavailableException.class)
                .hasMessageContaining("Google ADC 인증 정보를 찾을 수 없습니다.")
                .hasMessageContaining("gcloud auth application-default login")
                .hasMessageContaining("ADC 파일을 마운트");
    }
}
