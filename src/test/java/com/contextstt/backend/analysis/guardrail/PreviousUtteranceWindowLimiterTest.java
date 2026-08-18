package com.contextstt.backend.analysis.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.contextstt.backend.analysis.ContextAnalysisInput.PreviousUtterance;
import com.contextstt.backend.analysis.guardrail.PreviousUtteranceWindowLimiter.PreviousUtteranceWindow;
import java.util.List;
import org.junit.jupiter.api.Test;

class PreviousUtteranceWindowLimiterTest {

    @Test
    void keepsMostRecentContiguousUtterancesWithinCharacterBudget() {
        ContextAnalysisGuardrailProperties properties = properties(3, 6);
        PreviousUtteranceWindowLimiter limiter = new PreviousUtteranceWindowLimiter(properties);

        PreviousUtteranceWindow window = limiter.limit(List.of(
                utterance(0, "A", "1111"),
                utterance(1, "B", "22"),
                utterance(2, "C", "333"),
                utterance(3, "D", "4")
        ));

        assertThat(window.utterances())
                .extracting(PreviousUtterance::order)
                .containsExactly(2, 3);
        assertThat(window.omittedCount()).isEqualTo(2);
        assertThat(window.characterCount()).isEqualTo(6);
    }

    @Test
    void appliesUtteranceCountLimitEvenWhenCharacterBudgetRemains() {
        ContextAnalysisGuardrailProperties properties = properties(2, 100);
        PreviousUtteranceWindowLimiter limiter = new PreviousUtteranceWindowLimiter(properties);

        PreviousUtteranceWindow window = limiter.limit(List.of(
                utterance(0, "A", "첫째"),
                utterance(1, "B", "둘째"),
                utterance(2, "C", "셋째")
        ));

        assertThat(window.utterances())
                .extracting(PreviousUtterance::order)
                .containsExactly(1, 2);
        assertThat(window.omittedCount()).isOne();
    }

    private ContextAnalysisGuardrailProperties properties(int maxUtterances, int maxCharacters) {
        ContextAnalysisGuardrailProperties properties = new ContextAnalysisGuardrailProperties();
        properties.setMaxPreviousUtterances(maxUtterances);
        properties.setMaxPreviousCharacters(maxCharacters);
        return properties;
    }

    private PreviousUtterance utterance(int order, String speaker, String text) {
        return new PreviousUtterance(order, speaker, text);
    }
}
