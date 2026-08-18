# 맥락 분석 API

대화 발언을 Claude, Gemini 또는 DeepSeek로 분석해 가능한 해석과 추론 의도를 생성하고, 화자가 후보를 선택하거나 최종 표현을 직접 수정할 수 있습니다.

## 처리 흐름

1. STT 전사를 대화 발언에 연결합니다.
2. 초안 상태에서 맥락 분석을 요청합니다.
3. 점수가 높은 순서로 반환된 후보를 검토합니다.
4. 필요하면 STT 단어 교정 또는 재발언 API를 사용하고 다시 분석합니다.
5. 후보를 선택하거나 선택한 문구를 직접 수정합니다.
6. 전사와 맥락을 확인한 뒤 발언을 확정합니다.

분석 입력에는 대화 배경, 참여자, 이전에 확정된 발언, 대상 발언의 STT 원문과 현재 교정문이 포함됩니다. 기존 분석 결과는 당시 입력의 스냅샷이며 이후 전사 수정에 따라 자동으로 변경되지 않습니다.

분석 결과는 단어 ID와 order로 연결된 `ambiguities` 배열입니다. 맥락이 충분히 명확하면 `needsClarification`은 `false`, `ambiguities`는 빈 배열입니다. 한 발언에 여러 모호성 구간이 있으면 각 구간마다 독립적인 후보와 선택 결과가 생성됩니다.

## API

| 기능 | 메서드 | 경로 |
|---|---|---|
| 분석 생성 | `POST` | `/api/context-analyses` |
| 분석 상세 조회 | `GET` | `/api/context-analyses/{analysisId}` |
| 발언별 분석 이력 | `GET` | `/api/context-analyses?conversationId={id}&utteranceId={id}` |
| 구간별 후보 선택 | `PUT` | `/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/selection` |
| 구간별 선택 문구 수정 | `PATCH` | `/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/selection` |
| 구간 확정 | `PUT` | `/api/context-analyses/{analysisId}/ambiguities/{ambiguityId}/resolution` |

모든 API에는 Bearer access token이 필요합니다.

### 분석 생성

```http
POST /api/context-analyses
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "conversationId": 5,
  "utteranceId": 12,
  "candidateCount": 3,
  "model": "GEMINI_3_7_FLASH"
}
```

`candidateCount`는 모호성 구간마다 생성할 후보 수입니다. 생략하면 3이며 2에서 5 사이여야 합니다. 현재 요청은 선택한 모델의 응답이 완료될 때까지 기다리는 동기 방식입니다.

`model`을 생략하면 `CLAUDE_SONNET_5`를 사용합니다. 임의 모델 문자열은 허용하지 않으며 다음 값만 선택할 수 있습니다.

| 요청 값 | 호출 경로 | 실제 모델 ID |
|---|---|---|
| `CLAUDE_SONNET_5` | Anthropic API | `claude-sonnet-5` |
| `GEMINI_3_7_FLASH` | OpenRouter | `google/gemini-3.7-flash` |
| `DEEPSEEK_V4_FLASH` | OpenRouter | `deepseek/deepseek-v4-flash` |

응답의 `provider`에는 `ANTHROPIC` 또는 `OPENROUTER`, `model`에는 실제 호출한 모델 ID가 저장됩니다. OpenRouter 요청에는 strict JSON Schema를 사용하며 구조화 출력을 지원하는 제공자만 선택하도록 제한합니다.

### 후보 선택과 수정

```json
{
  "candidateId": 31
}
```

후보 선택 후 화자가 최종 맥락을 직접 수정할 수 있습니다.

```json
{
  "text": "거절하려는 것이 아니라 일정을 확인한 뒤 답하려는 뜻"
}
```

AI 후보가 맞지 않으면 후보를 먼저 선택하지 않고 직접 확정할 수 있습니다.

```json
{
  "type": "CUSTOM",
  "text": "주간 보고서를 내일까지 작성해 달라는 뜻"
}
```

AI가 표시한 구간이 실제로 모호하지 않다면 다음처럼 무시 처리합니다.

```json
{
  "type": "DISMISSED"
}
```

분석 후 단어를 수정하거나 재발언하면 해당 분석의 `stale`이 `true`가 됩니다. 오래된 분석에는 후보 선택·직접 확정을 적용할 수 없으며 현재 전사로 다시 분석해야 합니다. 모든 구간이 확정되고 분석이 최신이면 `usableResolution`이 `true`입니다.

## Claude 설정

AI 분석은 기본적으로 비활성화되어 있습니다.

| 환경 변수 | 기본값 | 설명 |
|---|---|---|
| `CLAUDE_API_ENABLED` | `false` | Claude 연동 활성화 여부 |
| `CLAUDE_API_KEY` | 없음 | Anthropic API 키 |
| `CLAUDE_API_BASE_URL` | `https://api.anthropic.com` | Messages API 기본 URL |
| `CLAUDE_API_MODEL` | `claude-sonnet-5` | 사용할 모델 |
| `CLAUDE_API_MAX_TOKENS` | `2048` | 응답 최대 토큰 수 |
| `CLAUDE_API_TIMEOUT_SECONDS` | `30` | 연결·응답 제한 시간 |

비활성화되었거나 API 키가 없으면 분석 생성 API는 `503 Service Unavailable`을 반환합니다. 제공자가 올바르지 않은 JSON이나 후보 데이터를 반환하면 `502 Bad Gateway`를 반환하며 분석 기록은 저장하지 않습니다.

## OpenRouter 설정

Gemini 3.7 Flash와 DeepSeek V4 Flash는 OpenRouter Chat Completions API를 사용합니다.

| 환경 변수 | 기본값 | 설명 |
|---|---|---|
| `OPENROUTER_API_ENABLED` | `false` | OpenRouter 연동 활성화 여부 |
| `OPENROUTER_API_KEY` | 없음 | OpenRouter API 키 |
| `OPENROUTER_API_BASE_URL` | `https://openrouter.ai/api/v1` | OpenRouter API 기본 URL |
| `OPENROUTER_GEMINI_MODEL` | `google/gemini-3.7-flash` | Gemini 모델 ID |
| `OPENROUTER_DEEPSEEK_MODEL` | `deepseek/deepseek-v4-flash` | DeepSeek 모델 ID |
| `OPENROUTER_API_MAX_TOKENS` | `2048` | 응답 최대 토큰 수 |
| `OPENROUTER_API_TIMEOUT_SECONDS` | `30` | 연결·응답 제한 시간 |
| `OPENROUTER_SITE_URL` | 없음 | 선택적인 앱 출처 URL |
| `OPENROUTER_APP_TITLE` | `Context STT` | OpenRouter 앱 표시 이름 |

OpenRouter 모델을 선택했는데 연동이 비활성화되었거나 API 키가 없으면 `503 Service Unavailable`을 반환합니다.

## 요청·입력 보호

외부 AI 비용과 긴 프롬프트를 제한하기 위해 사용자별 요청 윈도우와 이전 발언 컨텍스트 예산을 적용합니다. 한도를 초과한 분석 요청에는 `429 Too Many Requests`와 `Retry-After` 헤더를 반환합니다. 이전 발언은 가장 최근의 연속된 발언부터 개수·문자 예산 안에서 선택하며, 제외된 발언 수는 AI 입력의 `omittedPreviousUtteranceCount`에 포함됩니다. 현재 요청 윈도우는 애플리케이션 인스턴스 메모리에 저장되므로 다중 인스턴스 배포에서는 Redis 같은 공유 저장소로 교체해야 합니다.

| 환경 변수 | 기본값 | 설명 |
|---|---|---|
| `CONTEXT_ANALYSIS_MAX_PREVIOUS_UTTERANCES` | `20` | AI에 전달할 최대 이전 발언 수 |
| `CONTEXT_ANALYSIS_MAX_PREVIOUS_CHARACTERS` | `12000` | 이전 발언 화자명·본문의 최대 문자 수 |
| `CONTEXT_ANALYSIS_RATE_LIMIT_CAPACITY` | `10` | 사용자별 윈도우 내 최대 분석 요청 수 |
| `CONTEXT_ANALYSIS_RATE_LIMIT_WINDOW` | `1m` | 요청 제한 윈도우 |
| `CONTEXT_ANALYSIS_RATE_LIMIT_MAXIMUM_TRACKED_USERS` | `10000` | 메모리에 추적할 최대 사용자 수 |
