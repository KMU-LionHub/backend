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

| 변수 | 기본값 | 설명 |
|---|---|---|
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
