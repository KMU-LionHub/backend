package com.contextstt.backend.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contextstt.backend.exception.AnalysisProviderUnavailableException;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoutingContextAnalysisGatewayTest {

    @Test
    void delegatesToProviderSupportingRequestedModel() {
        ContextAnalysisProvider claude = mock(ContextAnalysisProvider.class);
        ContextAnalysisProvider openRouter = mock(ContextAnalysisProvider.class);
        ContextAnalysisInput input = input();
        ContextAnalysisResult expected = new ContextAnalysisResult("OPENROUTER", "model", List.of());
        when(claude.supports(ContextAnalysisModel.GEMINI_3_7_FLASH)).thenReturn(false);
        when(openRouter.supports(ContextAnalysisModel.GEMINI_3_7_FLASH)).thenReturn(true);
        when(openRouter.analyze(input, 3, ContextAnalysisModel.GEMINI_3_7_FLASH))
                .thenReturn(expected);

        RoutingContextAnalysisGateway gateway = new RoutingContextAnalysisGateway(
                List.of(claude, openRouter)
        );

        ContextAnalysisResult result = gateway.analyze(
                input,
                3,
                ContextAnalysisModel.GEMINI_3_7_FLASH
        );

        assertThat(result).isSameAs(expected);
        verify(openRouter).analyze(input, 3, ContextAnalysisModel.GEMINI_3_7_FLASH);
    }

    @Test
    void rejectsModelWhenNoProviderSupportsIt() {
        ContextAnalysisProvider provider = mock(ContextAnalysisProvider.class);
        ContextAnalysisInput input = input();
        when(provider.supports(ContextAnalysisModel.DEEPSEEK_V4_FLASH)).thenReturn(false);
        RoutingContextAnalysisGateway gateway = new RoutingContextAnalysisGateway(List.of(provider));

        assertThatThrownBy(() -> gateway.analyze(
                input,
                3,
                ContextAnalysisModel.DEEPSEEK_V4_FLASH
        ))
                .isInstanceOf(AnalysisProviderUnavailableException.class)
                .hasMessage("선택한 맥락 분석 모델을 지원하는 제공자가 없습니다.");
    }

    private ContextAnalysisInput input() {
        return new ContextAnalysisInput(
                1L,
                2L,
                null,
                List.of(),
                List.of(),
                "화자",
                "원문",
                "현재 문장",
                List.of(new ContextAnalysisInput.AnalysisWord(0, "현재 문장"))
        );
    }
}
