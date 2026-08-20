# Context STT Backend

정보 손실 없는 대화 도우미(Context STT) 백엔드. Spring Boot + Spring Data JPA(MySQL) + Spring Security(JWT) 기반 구현

## 기술 스택

- Java 21, Spring Boot 4.1
- Spring Data JPA / MySQL 8.4
- Spring Security (JWT, stateless)
- Google Speech-to-Text V2, Claude API, OpenRouter (Gemini / DeepSeek)
- Docker Compose, Caddy (리버스 프록시)
- Gradle

## 로컬 실행

### 1) Docker Compose (권장)

`.env.sample`을 복사해 `.env`를 만들고 필요한 값을 채웁니다.

```bash
cp .env.sample .env
docker compose up -d
```

`compose.yaml` + `compose.override.yaml`이 자동 적용되어 MySQL과 백엔드를 함께 로컬 빌드로 띄웁니다.

### 2) MySQL만 띄우고 Gradle로 직접 실행

```sql
CREATE DATABASE context_stt CHARACTER SET utf8mb4;
```

`.env.sample`을 참고해 환경 변수를 설정한 뒤 `./gradlew bootRun`으로 실행합니다.

## 환경 변수

전체 목록과 기본값은 [`.env.sample`](./.env.sample)을 참고하세요. 주요 항목:

| 변수 | 설명 |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 접속 정보 |
| `JWT_SECRET` | HS256 서명 키. 운영 배포 전 반드시 변경 (32바이트 이상) |
| `JWT_ACCESS_TOKEN_VALIDITY_MS` | Access Token 유효시간 |
| `CORS_ALLOWED_ORIGINS` | 허용할 프론트엔드 오리진 (쉼표 구분) |
| `AUTH_RATE_LIMIT_*` | 회원가입/로그인 요청 제한 (용량, 윈도우) |
| `STT_GOOGLE_ENABLED`, `GOOGLE_CLOUD_PROJECT`, `STT_GOOGLE_LOCATION`, `STT_GOOGLE_MODEL`, `STT_MAX_AUDIO_BYTES` | Google STT V2 연동 설정 |
| `CLAUDE_API_ENABLED`, `CLAUDE_API_KEY`, `CLAUDE_API_MODEL` 등 | 맥락 분석용 Claude API 설정 |
| `OPENROUTER_API_ENABLED`, `OPENROUTER_API_KEY`, `OPENROUTER_GEMINI_MODEL`, `OPENROUTER_DEEPSEEK_MODEL` 등 | 맥락 분석용 OpenRouter(Gemini/DeepSeek) 설정 |
| `CONTEXT_ANALYSIS_*` | 맥락 분석 히스토리 길이 및 요청 제한 |
| `SERVER_PORT` | 서버 포트 |

## API 문서

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- [맥락 분석 API](./docs/context-analysis-api.md)
- [STT API](./docs/stt-api.md)

로그인 후 발급받은 `accessToken`을 Swagger UI 우측 상단 `Authorize` 버튼에 입력하면 인증이 필요한 API도 바로 테스트할 수 있습니다.

## 배포

`main` 브랜치에 push되면 GitHub Actions(`.github/workflows/build.yml`)가 다음을 수행합니다.

1. Gradle 빌드 및 테스트
2. 커밋 SHA 태그로 이미지를 빌드해 `ghcr.io/kmu-lionhub/backend`에 push
3. self-hosted 러너에서 `compose.yaml` + `compose.prod.yaml`로 `docker compose up -d` 배포

운영 환경은 Caddy가 `api.wlstmd.cloud`로 들어오는 요청을 백엔드 컨테이너로 리버스 프록시합니다 (`Caddyfile`).

## 다음 단계로 고려할 것들

- Refresh Token / 로그아웃(토큰 블랙리스트) 흐름
- 이메일 인증, 비밀번호 재설정
