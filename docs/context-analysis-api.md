# AI 맥락 분석 API 구현 공유

STT 전사 결과(`currentText`)를 Claude로 분석해서, 나중에 다시 보면 의미가 불분명해질 수 있는 발언 구간과 그 맥락 후보를 추론하는 API를 추가했습니다.

STT는 이미 구현되어 있는 것을 그대로 입력으로 사용합니다. 이번 구현 범위는 분석 요청, 비동기 처리, 진행 상태/결과 폴링까지입니다.

## 구현 범위

| 기능 | 메서드 | 경로 |
|---|---|---|
| 맥락 분석 요청 | `POST` | `/api/analysis` |
| 분석 상태/결과 조회 | `GET` | `/api/analysis/{analysisId}` |

두 API 모두 로그인이 필요합니다.

```http
Authorization: Bearer <accessToken>
```

## 왜 비동기 + 폴링인가

Claude 호출은 초 단위로 걸릴 수 있어서 요청 스레드를 그대로 붙잡아두지 않습니다.

1. `POST /api/analysis`로 `transcriptionId`를 보내면 `analysisId`와 `PENDING` 상태를 즉시 반환합니다(202).
2. 실제 분석은 별도 스레드 풀에서 처리합니다.
3. 클라이언트는 `GET /api/analysis/{analysisId}`를 폴링하며 `status`/`progress`로 화면을 갱신합니다.

## 진행률(progress) 계산 방식

프론트에서 보여줄 진행률은 타이머로 흉내 낸 값이 아니라, 실제로 완료된 처리 단계마다 갱신됩니다.

| progress | 시점 |
|---:|---|
| 0 | 분석 요청 접수 직후 (`PENDING`) |
| 10 | 백그라운드 작업이 큐에서 꺼내져 시작됨 |
| 40 | Claude API에 요청을 보내기 직전 |
| 90 | Claude 응답을 받아 저장을 시작하기 직전 |
| 100 | 결과 저장 완료 (`COMPLETED`) |

실패하면 `status`가 `FAILED`로 바뀌고 `progress`는 마지막으로 도달한 단계 값에 머무릅니다. 실패 사유는 `errorMessage`에 담깁니다.

## 응답 형태

`GET /api/analysis/{analysisId}` 예시 (완료 시):

```json
{
  "id": 7,
  "transcriptionId": 21,
  "status": "COMPLETED",
  "progress": 100,
  "contextCandidates": [
    {
      "excerpt": "그거 이번 주까지 하기로 한 거 맞죠?",
      "candidates": [
        { "content": "이전 발언에서 언급된 특정 작업/업무를 지칭", "confidence": 0.7 },
        { "content": "직전 회의에서 결정된 일정 항목을 지칭", "confidence": 0.3 }
      ]
    }
  ],
  "errorMessage": null,
  "createdAt": "2026-08-18T21:00:00",
  "updatedAt": "2026-08-18T21:00:04"
}
```

`contextCandidates`는 분석이 끝나기 전까지 `null`입니다. 맥락 손실 위험이 없다고 판단되면 빈 배열을 반환합니다.

## Claude 호출 방식

- `spring-boot-starter-webmvc`에 포함된 `RestClient`로 Anthropic Messages API(`POST /v1/messages`)를 직접 호출합니다. 별도 SDK 의존성을 추가하지 않았습니다.
- 시스템 프롬프트에서 정해진 JSON 스키마만 출력하도록 강하게 지시하고, 응답 텍스트를 그대로 파싱합니다. 코드블록(````json`)으로 감싸서 응답하는 경우를 대비해 파싱 전에 한 번 벗겨냅니다.
- 스키마와 다른 형식으로 응답하면 분석이 `FAILED` 처리됩니다(재시도는 아직 없음).

## 설정

AI 분석 연동은 기본적으로 비활성화되어 있습니다.

| 환경 변수 | 기본값 | 설명 |
|---|---:|---|
| `CLAUDE_API_ENABLED` | `false` | AI 맥락 분석 활성화 여부 |
| `CLAUDE_API_KEY` | 없음 | Anthropic API 키. **절대 커밋 금지** |
| `CLAUDE_API_MODEL` | `claude-sonnet-5` | 사용할 Claude 모델 |
| `CLAUDE_API_MAX_TOKENS` | `1024` | 응답 최대 토큰 수 |
| `CLAUDE_API_TIMEOUT_SECONDS` | `30` | 연결/응답 타임아웃(초) |

`CLAUDE_API_KEY`는 `.env.sample`에는 빈 값만 있고, 실제 키는 gitignore 대상인 로컬 `.env`에만 넣습니다.

## 사용자 소유권 확인

분석 요청 시 `transcriptionId`가 요청자 소유가 아니면 `404`를 반환합니다(STT API와 동일한 방식). 분석 결과 조회도 분석을 요청한 사용자만 가능합니다.

## 다음에 고려할 것

- Claude 호출 실패 시 재시도
- 맥락 후보 중 하나를 사용자가 선택해서 확정하는 API
- 같은 전사에 대해 중복 분석 요청 제한
