# Context STT Backend

정보 손실 없는 대화 도우미(Context STT) 백엔드. Spring Boot + Spring Data JPA(MySQL) + Spring Security(JWT) 기반 구현

## 기술 스택

- Java 21, Spring Boot 4.1
- Spring Data JPA / MySQL 8
- Spring Security (JWT, stateless)
- Gradle

## 실행 준비

1. MySQL에 데이터베이스 생성

```sql
CREATE DATABASE context_stt CHARACTER SET utf8mb4;
```

2. 환경 변수 설정

`.env.sample`을 복사해 `.env`로 만들고 값을 채웁니다. `.env`는 gitignore 대상이라 커밋되지 않습니다.

```sh
cp .env.sample .env
```

#### DB / Docker Compose

| 변수 | 기본값(샘플) | 설명 |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL 호스트 |
| `DB_PORT` | `3306` | MySQL 포트 |
| `DB_NAME` | `context_stt` | 데이터베이스명 |
| `DB_USERNAME` | `context_stt` | DB 전용 계정 (root 아님) |
| `DB_PASSWORD` | - | DB 비밀번호 |
| `MYSQL_ROOT_PASSWORD` | - | `compose.yaml`이 MySQL 컨테이너를 초기화할 때만 사용. 앱은 root로 접속하지 않음 |
| `SPRING_PROFILES_ACTIVE` | `local` | `compose.yaml`에서 백엔드 컨테이너에 주입하는 Spring 프로필 |

#### Flyway / JPA

| 변수 | 기본값 | 설명 |
|---|---|---|
| `FLYWAY_BASELINE_ON_MIGRATE` | `false` | 기존 DB에 스키마 히스토리 없이 마이그레이션을 얹을지 여부 |
| `JPA_SHOW_SQL` | `false` | Hibernate SQL 로깅 |
| `JPA_FORMAT_SQL` | `false` | SQL 로그 포맷팅 |

#### 서버 / 인증(JWT)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8080` | 서버 포트 |
| `JWT_SECRET` | - | HS256 서명 키(Base64, 32바이트 이상). 운영 배포 전 반드시 변경 |
| `JWT_ACCESS_TOKEN_VALIDITY_MS` | `3600000` | Access Token 유효시간(ms) |

#### 로그인/회원가입 Rate Limit

| 변수 | 기본값 | 설명 |
|---|---|---|
| `AUTH_RATE_LIMIT_MAXIMUM_TRACKED_KEYS` | `20000` | 추적할 최대 키(IP 등) 개수 |
| `AUTH_RATE_LIMIT_SIGNUP_CAPACITY` | `5` | 회원가입 허용 횟수 |
| `AUTH_RATE_LIMIT_SIGNUP_WINDOW` | `1m` | 회원가입 제한 시간 창 |
| `AUTH_RATE_LIMIT_LOGIN_CAPACITY` | `10` | 로그인 허용 횟수 |
| `AUTH_RATE_LIMIT_LOGIN_WINDOW` | `1m` | 로그인 제한 시간 창 |

#### STT (Google Speech-to-Text)

기본적으로 비활성화되어 있습니다. 상세 내용은 [`docs/stt-api.md`](docs/stt-api.md) 참고.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `STT_GOOGLE_ENABLED` | `false` | Google STT 활성화 여부 |
| `GOOGLE_CLOUD_PROJECT` | - | Google Cloud 프로젝트 ID |
| `STT_GOOGLE_LOCATION` | `asia-northeast1` | STT 요청 리전 |
| `STT_GOOGLE_MODEL` | `long` | 사용할 인식 모델 |
| `STT_MAX_AUDIO_BYTES` | `10485760` | 허용할 최대 오디오 크기(byte) |

#### AI 맥락 분석 (Claude)

기본적으로 비활성화되어 있습니다. 상세 내용은 [`docs/context-analysis-api.md`](docs/context-analysis-api.md) 참고.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `CLAUDE_API_ENABLED` | `false` | AI 맥락 분석 활성화 여부 |
| `CLAUDE_API_KEY` | - | Anthropic API 키. **절대 커밋 금지** — 로컬 `.env`에만 설정 |
| `CLAUDE_API_BASE_URL` | `https://api.anthropic.com` | Claude API 엔드포인트 |
| `CLAUDE_API_MODEL` | `claude-sonnet-5` | 사용할 Claude 모델 |
| `CLAUDE_API_MAX_TOKENS` | `1024` | 응답 최대 토큰 수 |
| `CLAUDE_API_TIMEOUT_SECONDS` | `30` | 연결/응답 타임아웃(초) |

## API 문서 (Swagger)

서버 실행 후 아래 주소에서 확인할 수 있습니다.

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

로그인 후 발급받은 `accessToken`을 Swagger UI 우측 상단 `Authorize` 버튼에 입력하면 인증이 필요한 API도 바로 테스트할 수 있습니다.

## AI 맥락 분석 (Claude)

전사 기록(`currentText`)을 Claude에 보내 맥락 손실 위험이 있는 발언 구간과 그 맥락 후보를 추론합니다. 분석은 비동기로 처리하고, 클라이언트는 반환된 `analysisId`로 상태를 폴링합니다.

| 기능 | 메서드 | 경로 |
|---|---|---|
| 맥락 분석 요청 | `POST` | `/api/analysis` |
| 분석 상태/결과 조회 | `GET` | `/api/analysis/{analysisId}` |

상세 구현 내용, 진행률 산정 기준, 응답 JSON 스키마는 [`docs/context-analysis-api.md`](docs/context-analysis-api.md)를 참고하세요.

## 다음 단계로 고려할 것들

- Refresh Token / 로그아웃(토큰 블랙리스트) 흐름
- 이메일 인증, 비밀번호 재설정
- 맥락 후보 중 하나를 사용자가 선택/확정하는 UX 및 API
