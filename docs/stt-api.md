# STT API 설계

## 설계 원칙

- Google Speech-to-Text V2의 결과인 `originalText`는 수정하지 않는다.
- 화자의 단어 교정은 `correctedText`에 따로 저장하고, 후속 화면과 분석에는 `currentText`를 사용한다.
- 단어별 시작·종료 시각과 신뢰도를 저장해 프론트가 오디오 위치 이동과 검토 우선순위 표시에 활용할 수 있게 한다.
- 재발언은 기존 기록을 덮어쓰지 않고 새 전사를 만들며 `replacesTranscriptionId`로 연결한다.
- 민감할 수 있는 원본 오디오는 현재 DB에 저장하지 않는다. 요청 처리 중 Google STT에 전달한 뒤 폐기한다.
- 모든 전사 조회와 수정은 JWT 사용자 소유권을 확인한다. 다른 사용자의 ID를 요청해도 `404`를 반환한다.

## API 계약

모든 경로에는 `Authorization: Bearer <accessToken>` 헤더가 필요하다.

| 기능 | 메서드와 경로 | 요청 | 성공 |
|---|---|---|---|
| 새 발화 전사 | `POST /api/stt/transcriptions` | multipart `audio`, `languageCode`(기본 `ko-KR`) | `201` |
| 전사 조회 | `GET /api/stt/transcriptions/{id}` | 없음 | `200` |
| 단어 교정 | `PATCH /api/stt/transcriptions/{id}/words/{wordId}` | `{"text":"교정 단어"}` | `200` |
| 재발언 전사 | `POST /api/stt/transcriptions/{id}/re-record` | multipart `audio`, `languageCode` | `201` |

동기 전사 오디오는 60초 미만, 최대 10MB다. 자동 디코딩이 가능한 대표 형식은 WAV, FLAC, MP3, OGG/Opus, WebM/Opus, MP4/M4A다. 브라우저 `MediaRecorder`에서는 지원 여부를 확인한 뒤 `audio/webm;codecs=opus`를 우선 사용하는 구성이 편하다.

주요 오류 상태는 다음과 같다.

| 상태 | 의미 | 권장 UI 처리 |
|---|---|---|
| `400` | 빈 파일, 잘못된 언어 코드, 지원하지 않는 파일 | 입력을 바로 고칠 수 있게 안내 |
| `401` | 로그인 만료 또는 잘못된 토큰 | 재로그인 흐름 |
| `404` | 전사 또는 단어가 없거나 사용자 소유가 아님 | 목록/현재 화면을 새로고침 |
| `409` | 같은 전사를 동시에 교정 | 최신 기록을 다시 조회하고 재적용 |
| `413` | 10MB 제한 초과 | 더 짧게 녹음하도록 안내 |
| `422` | 음성 발화를 찾지 못함 | 주변 소음과 마이크를 확인하고 재녹음 |
| `503` | STT 비활성화, 인증 설정 오류, Google 일시 장애 | 재시도 버튼과 잠시 후 안내 |

응답의 핵심 형태는 다음과 같다.

```json
{
  "id": 21,
  "replacesTranscriptionId": null,
  "originalText": "정보 손실 없는 대화",
  "currentText": "정보 손실 없는 대화",
  "languageCode": "ko-KR",
  "provider": "GOOGLE_SPEECH_V2",
  "model": "long",
  "confidence": 0.94,
  "words": [
    {
      "id": 101,
      "order": 0,
      "originalText": "정보",
      "correctedText": null,
      "currentText": "정보",
      "startOffsetMillis": 120,
      "endOffsetMillis": 430,
      "confidence": 0.97
    }
  ]
}
```

## 권장 프론트 흐름

한 화면에 녹음, 검토, 확정 상태를 모두 쌓기보다 단계가 분명한 흐름이 좋다.

1. 녹음 상태에서는 큰 시작/정지 버튼, 입력 레벨, 60초 카운트다운을 보여준다.
2. 정지 후 오디오를 바로 업로드하며 파형 영역에 처리 상태를 표시한다.
3. 전사 결과는 문장과 단어 칩을 함께 보여준다. 신뢰도가 낮은 단어는 “틀림”으로 단정하지 말고 검토가 필요하다는 색과 아이콘만 사용한다.
4. 단어를 누르면 해당 `startOffsetMillis`로 오디오를 이동하고 작은 교정 입력창을 연다. 저장 시 단어 교정 API를 호출한다.
5. 결과가 전체적으로 다르면 “다시 말하기”로 재발언 API를 호출한다. 이전 기록은 복구 가능한 상태로 남는다.
6. 화자가 “이 문장으로 확정”을 누른 뒤에만 분석 API로 넘어간다.

모바일에서는 하단 고정 녹음 버튼과 세로 단어 흐름이 자연스럽고, 데스크톱에서는 왼쪽 파형/재생기와 오른쪽 전사 검토 패널의 2열 구성이 적합하다. 색만으로 신뢰도를 표현하지 말고 아이콘이나 라벨을 함께 사용한다.

## 후속 분석 API와의 연결

분석 API는 클라이언트가 보낸 임의 문장 대신 `transcriptionId`를 입력받고 서버에서 다음 값을 읽는 형태가 안전하다.

- 모델 입력 기본값: 화자 교정을 반영한 `currentText`
- 근거와 감사 기록: 변경되지 않는 `originalText`
- 불확실성 표시: 단어별 `confidence`와 교정 여부
- 재발언 추적: `replacesTranscriptionId`

예를 들어 `POST /api/analyses`에 `transcriptionId`와 필요한 대화 참여자/상황 메타데이터를 받고, 분석 후보에는 순위, 의도 설명, 근거 구간, 모델 점수를 별도 저장할 수 있다. STT 신뢰도와 “의도 유사도”는 의미가 다르므로 하나의 점수로 합치지 않는 편이 좋다.

실시간 자막이 필요해지면 현재 업로드 API를 억지로 확장하지 않고 별도의 WebSocket 또는 스트리밍 전사 경계를 추가한다. 60초를 넘는 녹음은 Cloud Storage와 비동기 `BatchRecognize` 작업 API로 분리하는 것이 적합하다.
