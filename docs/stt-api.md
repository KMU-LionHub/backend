# STT API 구현 공유

발화 녹음 파일을 Google Speech-to-Text V2로 전사하고, 결과를 단어 단위로 확인하거나 수정할 수 있는 API를 추가했습니다.

이번 구현 범위는 짧은 발화 업로드, 전사 결과 조회, 단어 교정, 재발언까지입니다. 실시간 자막이나 장시간 녹음은 아직 포함하지 않았습니다.

## 구현 범위

| 기능 | 메서드 | 경로 |
|---|---|---|
| 새 발화 전사 | `POST` | `/api/stt/transcriptions` |
| 전사 결과 조회 | `GET` | `/api/stt/transcriptions/{transcriptionId}` |
| 단어 교정 | `PATCH` | `/api/stt/transcriptions/{transcriptionId}/words/{wordId}` |
| 재발언 전사 | `POST` | `/api/stt/transcriptions/{transcriptionId}/re-record` |

네 API 모두 로그인이 필요합니다. 로그인 API에서 받은 Access Token을 아래와 같이 전달하면 됩니다.

```http
Authorization: Bearer <accessToken>
```

## 처리 방식

전체 흐름은 다음과 같습니다.

1. 클라이언트가 녹음을 끝낸 뒤 오디오 파일을 `multipart/form-data`로 전송합니다.
2. 백엔드가 파일 크기, 미디어 타입, 언어 코드를 확인합니다.
3. Google Speech-to-Text V2에 오디오를 전달합니다.
4. 전사 문장과 단어별 시작·종료 시각, 신뢰도를 변환합니다.
5. 전사 원문과 단어 정보를 DB에 저장합니다.
6. 저장된 전사 ID와 단어 ID를 클라이언트에 반환합니다.
7. 클라이언트는 단어 ID를 이용해 잘못 인식된 단어를 수정할 수 있습니다.

원본 오디오 바이트는 Google STT 호출에만 사용합니다. 현재 백엔드 DB나 파일 시스템에는 저장하지 않습니다.

## 구현하면서 정한 기준

### STT 원문 보존

Google이 처음 반환한 문장은 `originalText`에 저장하고 이후에도 그대로 유지합니다.

사용자 교정이 반영된 문장은 `currentText`에 따로 저장합니다. 추후 분석 API를 연결할 때는 기본 입력으로 `currentText`를 사용하고, 원본 확인이 필요한 경우 `originalText`를 확인하면 됩니다.

단어도 같은 방식으로 관리합니다.

- `originalText`: STT가 인식한 단어
- `correctedText`: 사용자가 수정한 단어. 수정 전에는 `null`
- `currentText`: 화면과 후속 분석에서 사용할 단어

### 재발언 기록 보존

재발언하면 기존 행을 덮어쓰지 않고 새 전사 기록을 만듭니다. 새 기록의 `replacesTranscriptionId`에는 이전 전사 ID가 들어갑니다.

예를 들어 21번 전사를 다시 녹음해서 22번 전사가 생성되면 다음과 같이 연결됩니다.

```text
22번 전사 --replaces--> 21번 전사
```

기존 21번 기록도 계속 조회할 수 있습니다.

### 사용자 소유권 확인

조회, 수정, 재발언 모두 JWT의 사용자 ID와 전사 기록의 사용자 ID가 같은지 확인합니다.

다른 사용자의 전사 ID를 요청한 경우에는 해당 리소스의 존재 여부를 노출하지 않고 `404 Not Found`를 반환합니다.

## Google Cloud 설정

Google STT 연동은 기본적으로 비활성화되어 있습니다. 로컬이나 배포 환경에서 아래 설정을 입력해야 실제 호출이 가능합니다.

| 환경 변수 | 기본값 | 설명 |
|---|---:|---|
| `STT_GOOGLE_ENABLED` | `false` | Google STT 활성화 여부 |
| `GOOGLE_CLOUD_PROJECT` | 없음 | Google Cloud 프로젝트 ID |
| `STT_GOOGLE_LOCATION` | `asia-northeast1` | STT 요청 리전 |
| `STT_GOOGLE_MODEL` | `long` | 사용할 인식 모델 |
| `STT_MAX_AUDIO_BYTES` | `10485760` | 허용할 최대 오디오 크기 |

로컬에서는 먼저 ADC 인증이 필요합니다.

```bash
gcloud auth application-default login
```

실행 예시는 다음과 같습니다.

```bash
export STT_GOOGLE_ENABLED=true
export GOOGLE_CLOUD_PROJECT=<project-id>
export STT_GOOGLE_LOCATION=asia-northeast1
export STT_GOOGLE_MODEL=long

./gradlew bootRun --args='--spring.profiles.active=local'
```

Google Cloud 프로젝트에서는 Speech-to-Text API를 활성화하고, 애플리케이션 실행 계정에 최소 `Cloud Speech Client` 역할을 부여해야 합니다.

운영 환경에서는 서비스 계정 키 JSON을 저장소에 올리지 않습니다. 가능하면 실행 환경에 연결된 서비스 계정이나 Workload Identity를 사용합니다.

## 오디오 요청 제한

현재 API는 Google의 동기 전사 방식인 `Recognize`를 사용합니다.

- 최대 파일 크기: 10MB
- 최대 길이: 60초
- 기본 언어: `ko-KR`
- 언어 코드 형식: BCP-47
- 대표 지원 형식: WAV, FLAC, MP3, OGG/Opus, WebM/Opus, MP4/M4A

Spring multipart 설정은 오디오 10MB, 전체 요청 11MB로 맞춰 두었습니다.

브라우저에서 녹음할 때는 지원 여부를 확인한 뒤 `audio/webm;codecs=opus`를 우선 사용하면 됩니다. `FormData`를 전송할 때 `Content-Type` 헤더는 직접 만들지 않고 브라우저가 boundary와 함께 설정하도록 두는 편이 안전합니다.

## 호출 전 로그인

이미 구현된 인증 API를 그대로 사용합니다.

### 회원가입

```http
POST /api/auth/signup
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "password1!",
  "nickname": "사용자"
}
```

성공 시 `201 Created`를 반환합니다.

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "사용자"
}
```

### 로그인

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "password1!"
}
```

성공 시 `200 OK`와 Access Token을 반환합니다.

```json
{
  "token": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresInSeconds": 3600
  },
  "user": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "사용자"
  }
}
```

이후 예제의 `<accessToken>` 부분에 위 값을 사용합니다.

## 1. 새 발화 전사

```http
POST /api/stt/transcriptions
Content-Type: multipart/form-data
Authorization: Bearer <accessToken>
```

### 요청 필드

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `audio` | file | O | 전사할 오디오 파일 |
| `languageCode` | string | X | 기본값 `ko-KR` |

### cURL 예시

```bash
curl -X POST 'http://localhost:8080/api/stt/transcriptions' \
  -H 'Authorization: Bearer <accessToken>' \
  -F 'audio=@speech.webm;type=audio/webm' \
  -F 'languageCode=ko-KR'
```

### 프론트 요청 예시

```javascript
const formData = new FormData();
formData.append("audio", audioBlob, "speech.webm");
formData.append("languageCode", "ko-KR");

const response = await fetch("/api/stt/transcriptions", {
  method: "POST",
  headers: {
    Authorization: `Bearer ${accessToken}`
  },
  body: formData
});

const transcription = await response.json();
```

### 성공 응답

`201 Created`

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
  "audioContentType": "audio/webm",
  "audioSizeBytes": 482310,
  "words": [
    {
      "id": 101,
      "order": 0,
      "originalText": "정보",
      "correctedText": null,
      "currentText": "정보",
      "startOffsetMillis": 120,
      "endOffsetMillis": 430,
      "confidence": 0.97,
      "speakerLabel": null,
      "correctedAt": null
    },
    {
      "id": 102,
      "order": 1,
      "originalText": "손실",
      "correctedText": null,
      "currentText": "손실",
      "startOffsetMillis": 440,
      "endOffsetMillis": 780,
      "confidence": 0.82,
      "speakerLabel": null,
      "correctedAt": null
    }
  ],
  "createdAt": "2026-08-18T21:35:10.123",
  "updatedAt": "2026-08-18T21:35:10.123"
}
```

### 응답 필드

| 필드 | 설명 |
|---|---|
| `id` | 전사 기록 ID |
| `replacesTranscriptionId` | 재발언으로 대체한 이전 기록 ID. 최초 전사는 `null` |
| `originalText` | Google STT가 반환한 원문 |
| `currentText` | 사용자 교정을 반영한 현재 문장 |
| `languageCode` | 전사에 사용한 언어 코드 |
| `provider` | 사용한 STT 제공자 |
| `model` | 사용한 Google STT 모델 |
| `confidence` | 문장 전체 신뢰도. 제공되지 않을 수 있음 |
| `audioContentType` | 업로드한 파일의 MIME 타입 |
| `audioSizeBytes` | 업로드한 오디오 크기 |
| `words` | 단어별 결과 목록 |
| `createdAt` | 생성 시각 |
| `updatedAt` | 마지막 수정 시각 |

단어의 `startOffsetMillis`, `endOffsetMillis`, `confidence`는 Google 응답에 값이 없으면 `null`일 수 있습니다. 프론트에서는 항상 nullable로 처리해야 합니다.

Google이 단어 목록을 반환하지 않은 경우에는 전체 문장을 공백 기준으로 나눠 단어 목록을 만듭니다. 이 경우 단어 수정은 가능하지만 타임스탬프와 신뢰도는 `null`입니다.

## 2. 전사 결과 조회

```http
GET /api/stt/transcriptions/{transcriptionId}
Authorization: Bearer <accessToken>
```

```bash
curl 'http://localhost:8080/api/stt/transcriptions/21' \
  -H 'Authorization: Bearer <accessToken>'
```

성공 시 `200 OK`와 새 발화 전사 API와 같은 형태의 전체 객체를 반환합니다.

전사 기록이 없거나 요청한 사용자의 기록이 아니면 `404 Not Found`를 반환합니다.

## 3. 단어 교정

```http
PATCH /api/stt/transcriptions/{transcriptionId}/words/{wordId}
Content-Type: application/json
Authorization: Bearer <accessToken>
```

프론트에서는 `words[].order`가 아니라 `words[].id`를 경로의 `wordId`로 사용해야 합니다.

### 요청 예시

```bash
curl -X PATCH 'http://localhost:8080/api/stt/transcriptions/21/words/102' \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "text": "손씰"
  }'
```

```json
{
  "text": "손씰"
}
```

교정 문자열은 공백만 입력할 수 없으며 최대 2,000자입니다.

### 성공 응답

`200 OK`와 교정이 반영된 전체 전사 객체를 반환합니다.

```json
{
  "id": 21,
  "originalText": "정보 손실 없는 대화",
  "currentText": "정보 손씰 없는 대화",
  "words": [
    {
      "id": 102,
      "order": 1,
      "originalText": "손실",
      "correctedText": "손씰",
      "currentText": "손씰",
      "startOffsetMillis": 440,
      "endOffsetMillis": 780,
      "confidence": 0.82,
      "speakerLabel": null,
      "correctedAt": "2026-08-18T21:37:21.456"
    }
  ]
}
```

위 JSON은 변경된 부분만 줄여서 작성한 예시입니다. 실제 응답에는 생성 API와 동일한 나머지 필드와 전체 단어 목록이 포함됩니다.

교정값을 원래 단어와 같게 보내면 수정 상태를 해제합니다.

```json
{
  "text": "손실"
}
```

이 경우 `correctedText`와 `correctedAt`은 다시 `null`이 되고, 단어의 `currentText`도 원문으로 돌아갑니다.

## 4. 재발언 전사

```http
POST /api/stt/transcriptions/{transcriptionId}/re-record
Content-Type: multipart/form-data
Authorization: Bearer <accessToken>
```

요청 형식은 새 발화 전사 API와 같습니다.

```bash
curl -X POST 'http://localhost:8080/api/stt/transcriptions/21/re-record' \
  -H 'Authorization: Bearer <accessToken>' \
  -F 'audio=@retry.webm;type=audio/webm' \
  -F 'languageCode=ko-KR'
```

성공 시 `201 Created`를 반환합니다.

```json
{
  "id": 22,
  "replacesTranscriptionId": 21,
  "originalText": "정보가 손실되지 않는 대화",
  "currentText": "정보가 손실되지 않는 대화",
  "languageCode": "ko-KR",
  "provider": "GOOGLE_SPEECH_V2",
  "model": "long",
  "confidence": 0.96,
  "audioContentType": "audio/webm",
  "audioSizeBytes": 510204,
  "words": [],
  "createdAt": "2026-08-18T21:40:00.123",
  "updatedAt": "2026-08-18T21:40:00.123"
}
```

프론트에서는 재발언 성공 후 현재 전사 ID를 새로 받은 `22`로 교체하면 됩니다. 기존 21번 기록은 삭제되지 않습니다.

## 오류 응답

오류 응답 형식은 기존 API와 같습니다.

```json
{
  "timestamp": "2026-08-18T21:41:00",
  "status": 400,
  "message": "오디오 파일은 필수입니다.",
  "errors": []
}
```

| 상태 | 발생하는 경우 | 클라이언트 처리 |
|---|---|---|
| `400` | 빈 파일, 잘못된 언어 코드, 지원하지 않는 미디어 타입 | 입력값을 확인하고 다시 요청 |
| `401` | 토큰 누락, 만료, 잘못된 토큰 | 로그인 화면으로 이동 |
| `404` | 전사나 단어가 없거나 다른 사용자의 기록 | 현재 데이터를 다시 조회 |
| `409` | 같은 전사를 동시에 수정 | 최신 데이터를 조회한 뒤 다시 교정 |
| `413` | 파일 크기가 제한을 초과 | 더 짧게 녹음하거나 파일 크기 축소 |
| `422` | 파일은 처리했지만 발화를 찾지 못함 | 마이크와 주변 소음을 확인하고 재녹음 |
| `503` | STT 비활성화, Google 인증 설정 오류, Google 일시 장애 | 설정 확인 또는 잠시 후 재시도 |

대표 오류 메시지는 다음과 같습니다.

```json
{
  "timestamp": "2026-08-18T21:41:00",
  "status": 422,
  "message": "음성에서 발화를 인식하지 못했습니다. 다시 녹음해 주세요.",
  "errors": []
}
```

```json
{
  "timestamp": "2026-08-18T21:41:00",
  "status": 503,
  "message": "STT 기능이 활성화되어 있지 않습니다.",
  "errors": []
}
```

## DB 변경 사항

Flyway `V3__create_transcriptions.sql`에서 두 테이블을 추가했습니다.

```text
users
  └─ transcriptions
       ├─ replaces_transcription_id -> transcriptions.id
       └─ transcript_words
```

### `transcriptions`

전사 한 건의 원문, 현재 문장, 모델 정보, 오디오 메타데이터를 저장합니다.

주요 컬럼은 다음과 같습니다.

- `user_id`: 전사 소유 사용자
- `replaces_transcription_id`: 재발언 시 이전 전사 ID
- `provider`, `model`, `language_code`: STT 호출 정보
- `original_text`: 변경하지 않는 STT 원문
- `current_text`: 사용자 교정을 반영한 문장
- `confidence`: 전체 문장 신뢰도
- `audio_content_type`, `audio_size_bytes`: 오디오 메타데이터
- `version`: 단어 교정 동시성 제어에 사용하는 낙관적 락 버전

### `transcript_words`

전사 결과의 단어별 정보를 저장합니다.

- `word_order`: 문장 내 단어 순서
- `original_text`: STT 원본 단어
- `corrected_text`: 사용자 교정값
- `start_offset_millis`, `end_offset_millis`: 오디오 내 위치
- `confidence`: 단어 신뢰도
- `speaker_label`: 화자 분리용 필드
- `corrected_at`: 마지막 교정 시각

전사 기록이 삭제되면 연결된 단어도 같이 삭제되도록 외래 키에 `ON DELETE CASCADE`를 설정했습니다. 현재 전사 삭제 API는 구현하지 않았습니다.

## 주요 코드 위치

- [SttController](../src/main/java/com/contextstt/backend/controller/SttController.java): HTTP 요청과 응답
- [TranscriptionService](../src/main/java/com/contextstt/backend/service/TranscriptionService.java): 파일 검증, 소유권 확인, 저장, 교정 처리
- [SpeechToTextGateway](../src/main/java/com/contextstt/backend/stt/SpeechToTextGateway.java): STT 제공자 추상화
- [GoogleSpeechToTextGateway](../src/main/java/com/contextstt/backend/stt/google/GoogleSpeechToTextGateway.java): Google Speech-to-Text V2 요청 및 응답 변환
- [Transcription](../src/main/java/com/contextstt/backend/domain/transcription/Transcription.java): 전사 엔티티와 단어 교정 로직
- [TranscriptWord](../src/main/java/com/contextstt/backend/domain/transcription/TranscriptWord.java): 단어 엔티티
- [V3 migration](../src/main/resources/db/migration/V3__create_transcriptions.sql): 테이블 생성

Google 이외의 STT를 연결하더라도 컨트롤러나 서비스가 제공자 SDK를 직접 참조하지 않도록 `SpeechToTextGateway` 인터페이스를 사이에 두었습니다.

## 프론트 연동 시 확인할 내용

- 화면에는 `originalText`가 아니라 `currentText`를 기본으로 표시합니다.
- 단어를 수정할 때는 `order`가 아니라 `wordId`를 사용합니다.
- 단어 타임스탬프와 신뢰도는 `null`일 수 있습니다.
- 신뢰도가 낮다고 무조건 오답은 아니므로, 검토가 필요한 단어를 표시하는 용도로 사용하는 것이 좋습니다.
- 단어를 선택하면 `startOffsetMillis` 위치로 오디오 재생 지점을 이동할 수 있습니다.
- 재발언 성공 후에는 새 응답의 `id`를 현재 전사 ID로 사용합니다.
- 기존 전사와 재발언 전사는 모두 서버에 남습니다.
- 원본 오디오는 서버에 남지 않으므로, 교정 화면에서 다시 재생하려면 프론트에서 녹음 Blob을 유지해야 합니다.

## 테스트

전체 테스트와 빌드는 아래 명령으로 확인할 수 있습니다.

```bash
./gradlew clean check bootJar
```

STT 관련 테스트는 다음 두 파일에 있습니다.

- [SttControllerTest](../src/test/java/com/contextstt/backend/controller/SttControllerTest.java)
- [GoogleSpeechToTextGatewayTest](../src/test/java/com/contextstt/backend/stt/google/GoogleSpeechToTextGatewayTest.java)

테스트에서는 실제 Google API를 호출하지 않고 `SpeechToTextGateway`를 모킹합니다. 따라서 Google 프로젝트와 ADC를 설정한 환경에서는 짧은 샘플 파일로 실제 호출을 한 번 확인해야 합니다.

Swagger는 `local` 프로필에서 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

## 현재 포함하지 않은 기능

- 실시간 스트리밍 전사
- 60초를 초과하는 장시간 오디오
- 원본 오디오 저장 및 다시 듣기
- 전사 목록 조회
- 전사 삭제
- 문장 전체를 한 번에 수정하는 API
- STT 전용 요청 횟수 제한
- 화자 분리 활성화
- 분석 API

실시간 전사는 현재 업로드 API에 덧붙이기보다 별도 WebSocket 또는 gRPC 스트리밍 경로로 분리하는 편이 좋습니다. 장시간 파일은 Cloud Storage와 `BatchRecognize`를 사용하는 비동기 작업 형태로 추가할 수 있습니다.

분석 API는 클라이언트가 임의 문장을 다시 보내는 방식보다 `transcriptionId`를 받아 서버에서 `originalText`와 `currentText`를 조회하는 형태로 연결할 예정입니다.
