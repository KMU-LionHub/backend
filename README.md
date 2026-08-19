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

| 변수 | 설명 |
|---|---|
| `DB_HOST` | MySQL 호스트 |
| `DB_PORT` | MySQL 포트 |
| `DB_NAME` | 데이터베이스명 |
| `DB_USERNAME` | DB 계정 |
| `DB_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | HS256 서명 키. 운영 배포 전 반드시 변경 (32바이트 이상) |
| `JWT_ACCESS_TOKEN_VALIDITY_MS` | Access Token 유효시간 |
| `SERVER_PORT` | 서버 포트 |

## API 문서 (Swagger)

서버 실행 후 아래 주소에서 확인할 수 있습니다.

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

로그인 후 발급받은 `accessToken`을 Swagger UI 우측 상단 `Authorize` 버튼에 입력하면 인증이 필요한 API도 바로 테스트할 수 있습니다.

## 다음 단계로 고려할 것들

- Refresh Token / 로그아웃(토큰 블랙리스트) 흐름
- 이메일 인증, 비밀번호 재설정


# Context STT Backend

음성 대화를 STT로 변환하고, 발언의 모호한 표현을 AI가 분석하여 여러 맥락 후보를 생성하고 확정할 수 있도록 지원하는 Backend 프로젝트입니다.

---

## 주요 기능

- 회원가입 / 로그인
- JWT 기반 인증
- 대화 생성
- 참여자 관리
- 발언 생성
- Google Speech-to-Text 연동
- STT 결과 저장
- AI 맥락 분석
- 맥락 후보 생성
- 맥락 후보 선택
- 직접 맥락 입력
- 최종 맥락 확정
- MySQL 기반 데이터 저장

---

## 기술 스택

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- Flyway
- Docker
- Docker Compose
- Google Cloud Speech-to-Text
- OpenRouter
- Gradle

---

# 로컬 실행 방법

## 1. 저장소 Clone

처음 프로젝트를 받는 경우:

```bash
git clone <BACKEND_REPOSITORY_URL>
```

프로젝트 폴더로 이동합니다.

```bash
cd backend
```

현재 통합 테스트 브랜치로 이동합니다.

```bash
git switch fix/cors-migration
```

최신 코드를 받아옵니다.

```bash
git pull origin fix/cors-migration
```

---

# 2. 환경 변수 설정

Backend 실행을 위해 로컬 환경 설정이 필요합니다.

프로젝트에서 제공하는 `.env.sample` 또는 환경 변수 예시를 참고하여 `.env` 파일을 생성합니다.

예:

```bash
cp .env.sample .env
```

`.env`에는 프로젝트 설정에 따라 다음과 같은 값들이 필요할 수 있습니다.

```env
SPRING_PROFILES_ACTIVE=local

DB_HOST=mysql
DB_PORT=3306
DB_NAME=context_stt
DB_USERNAME=context_stt
DB_PASSWORD=your-password

JWT_SECRET=your-jwt-secret

STT_GOOGLE_ENABLED=true
GOOGLE_CLOUD_PROJECT=your-project-id
STT_GOOGLE_LOCATION=global
STT_GOOGLE_MODEL=long

OPENROUTER_API_ENABLED=true
OPENROUTER_API_KEY=your-openrouter-api-key
```

> 실제 변수 이름은 현재 프로젝트의 `application.yml`, `application-local.yml`, `compose.yaml` 설정을 기준으로 확인해주세요.

---

# 3. Google Cloud STT 인증

Google Speech-to-Text를 사용하려면 Google Cloud 인증 정보가 필요합니다.

개인 서비스 계정 인증 JSON 파일은 GitHub에 업로드하지 않습니다.

로컬에서 필요한 경로에 인증 파일을 두고 `compose.yaml` 또는 환경 변수에서 해당 파일을 참조하도록 설정합니다.

예:

```text
google-credentials.json
```

중요:

```text
서비스 계정 JSON 파일은 절대 GitHub에 커밋하지 않습니다.
```

Google Cloud 프로젝트에서 Speech-to-Text API가 활성화되어 있어야 합니다.

---

# 4. OpenRouter API Key 설정

AI 맥락 분석 기능을 사용하기 위해 OpenRouter API Key가 필요합니다.

`.env` 또는 프로젝트에서 사용하는 환경 변수에 API Key를 설정합니다.

예:

```env
OPENROUTER_API_KEY=your-key
```

실제 API Key는 GitHub에 올리지 않습니다.

---

# 5. Docker 실행

Backend와 MySQL을 Docker Compose로 실행합니다.

프로젝트 루트에서:

```bash
docker compose up --build
```

백그라운드 실행:

```bash
docker compose up -d --build
```

---

# 6. 실행 확인

컨테이너 상태 확인:

```bash
docker compose ps
```

Backend 로그 확인:

```bash
docker compose logs backend --tail=100
```

실시간 로그:

```bash
docker compose logs -f backend
```

정상적으로 실행되면 Backend는 기본적으로 다음 주소에서 동작합니다.

```text
http://localhost:8080
```

로그에서 다음과 비슷한 메시지가 보이면 정상 실행입니다.

```text
Tomcat started on port 8080
Started BackendApplication
```

---

# Frontend 연결

Frontend는 기본적으로 다음 Backend 주소를 사용합니다.

```text
http://localhost:8080
```

따라서 전체 로컬 테스트를 위해서는 Backend를 먼저 실행한 뒤 Frontend를 실행하는 것을 권장합니다.

전체 구조:

```text
React Frontend
localhost:5173
      │
      ▼
Spring Boot Backend
localhost:8080
      │
      ├── MySQL
      ├── Google Speech-to-Text
      └── OpenRouter
```

---

# 전체 테스트 순서

## Backend

```bash
docker compose up --build
```

Backend가 정상 실행되는지 확인합니다.

```bash
docker compose ps
```

새 터미널에서 Frontend를 실행합니다.

## Frontend

```bash
npm install
npm run dev
```

이후 브라우저에서 다음 순서로 테스트합니다.

1. 회원가입
2. 로그인
3. 마이크 권한 허용
4. 녹음 시작
5. 음성 입력
6. 녹음 종료
7. STT 결과 확인
8. AI 맥락 후보 확인
9. 후보 선택
10. 필요한 경우 맥락 직접 수정
11. 맥락 최종 확정
12. 대화 기록 확인

---

# AI 맥락 확정 방식

Backend에서 모호성 구간은 다음 3가지 방식으로 확정할 수 있습니다.

## CANDIDATE

AI가 생성한 기존 후보를 그대로 확정합니다.

요청 예:

```json
{
  "type": "CANDIDATE",
  "candidateId": 10
}
```

이 경우:

```text
candidateId 필요
text 사용하지 않음
```

---

## CUSTOM

사용자가 직접 작성한 맥락으로 확정합니다.

요청 예:

```json
{
  "type": "CUSTOM",
  "text": "사용자가 직접 입력한 맥락"
}
```

이 경우:

```text
candidateId 사용하지 않음
text 필요
```

---

## DISMISSED

해당 모호성 구간을 확정하지 않고 제외합니다.

요청 예:

```json
{
  "type": "DISMISSED"
}
```

이 경우:

```text
candidateId 사용하지 않음
text 사용하지 않음
```

---

# 문제 해결

## Backend가 실행되지 않는 경우

컨테이너 상태 확인:

```bash
docker compose ps
```

로그 확인:

```bash
docker compose logs backend --tail=250
```

---

## Docker 재시작

```bash
docker compose down
```

다시 빌드:

```bash
docker compose up --build
```

---

## MySQL 문제

MySQL 로그 확인:

```bash
docker compose logs mysql --tail=200
```

컨테이너 확인:

```bash
docker compose ps
```

---

## Google STT 오류

실시간 Backend 로그 확인:

```bash
docker compose logs -f backend
```

Google STT 관련 오류 예:

```text
INVALID_ARGUMENT
UNAVAILABLE
UnknownHostException
```

다음을 확인해주세요.

- Google Cloud 인증 파일
- 프로젝트 ID
- Speech-to-Text API 활성화 여부
- 인터넷 연결
- Docker 컨테이너 DNS
- STT 모델 / location 설정

---

## AI 맥락 분석 오류

Backend 로그 확인:

```bash
docker compose logs backend --tail=250
```

다음을 확인해주세요.

- OpenRouter API Key
- OpenRouter API 연결
- 모델 설정
- AI 응답 형식
- JSON parsing 오류

---

## CORS 오류

Frontend가 다음과 같은 Vite 포트에서 실행될 수 있습니다.

```text
localhost:5173
localhost:5174
localhost:5175
localhost:5176
localhost:5177
localhost:5178
localhost:5179
```

현재 Backend Security 설정에서 허용된 Origin을 확인해주세요.

브라우저 Console에서 CORS 오류가 발생하면 Backend `SecurityConfig`의 CORS 설정을 확인합니다.

---

# Swagger

Swagger가 활성화되어 있다면 Backend 실행 후 API 명세를 확인할 수 있습니다.

일반적인 Springdoc Swagger UI 경로:

```text
http://localhost:8080/swagger-ui/index.html
```

프로젝트 설정에 따라 주소가 다를 수 있습니다.

---

# 주요 Backend 구조

```text
backend
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.contextstt.backend
│   │   │       ├── analysis
│   │   │       ├── controller
│   │   │       ├── domain
│   │   │       ├── dto
│   │   │       ├── exception
│   │   │       ├── repository
│   │   │       ├── security
│   │   │       ├── service
│   │   │       └── stt
│   │   └── resources
│   └── test
├── compose.yaml
├── Dockerfile
├── build.gradle
├── settings.gradle
└── README.md
```

---

# 보안 주의사항

다음 정보는 절대 GitHub에 커밋하지 않습니다.

```text
.env
Google Cloud 서비스 계정 JSON
OpenRouter API Key
JWT Secret
Database Password
개인 인증 정보
```

GitHub에 올리기 전 반드시 다음 명령으로 변경 파일을 확인해주세요.

```bash
git status
```

민감한 파일이 보이면 커밋하지 않습니다.

---

# 기존 팀원이 최신 코드 받기

이미 Backend 저장소가 있는 경우:

```bash
git fetch origin
```

브랜치 이동:

```bash
git switch fix/cors-migration
```

최신 코드:

```bash
git pull origin fix/cors-migration
```

환경 설정 확인 후:

```bash
docker compose up --build
```

---

# 개발 브랜치

현재 로컬 통합 테스트에 사용 중인 브랜치:

```text
fix/cors-migration
```

Frontend 통합 테스트 브랜치:

```text
feat/auth
```