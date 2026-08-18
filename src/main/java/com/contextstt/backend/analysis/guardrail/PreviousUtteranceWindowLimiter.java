package com.contextstt.backend.analysis.guardrail;

import com.contextstt.backend.analysis.ContextAnalysisInput.PreviousUtterance;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PreviousUtteranceWindowLimiter {

    private final int maxUtterances;
    private final int maxCharacters;

    public PreviousUtteranceWindowLimiter(ContextAnalysisGuardrailProperties properties) {
        this.maxUtterances = properties.getMaxPreviousUtterances();
        this.maxCharacters = properties.getMaxPreviousCharacters();
    }

    public PreviousUtteranceWindow limit(List<PreviousUtterance> utterances) {
        int lowerBound = Math.max(0, utterances.size() - maxUtterances);
        List<PreviousUtterance> selected = new ArrayList<>();
        int usedCharacters = 0;

        for (int index = utterances.size() - 1; index >= lowerBound; index--) {
            PreviousUtterance utterance = utterances.get(index);
            int characters = utterance.speakerName().length() + utterance.text().length();
            if (usedCharacters + characters > maxCharacters) {
                break;
            }
            selected.addFirst(utterance);
            usedCharacters += characters;
        }

        return new PreviousUtteranceWindow(
                selected,
                utterances.size() - selected.size(),
                usedCharacters
        );
    }

    public record PreviousUtteranceWindow(
            List<PreviousUtterance> utterances,
            int omittedCount,
            int characterCount
    ) {

        public PreviousUtteranceWindow {
            utterances = List.copyOf(utterances);
        }
    }
}
