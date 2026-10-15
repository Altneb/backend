# Altneb Backend

Java 21과 Spring Boot 기반의 Gradle 멀티모듈 모듈형 모놀리스입니다.
실행 가능한 애플리케이션은 `apps/api-server` 하나이며 나머지 모듈은 라이브러리입니다.

## 요구 사항

- JDK 21
- 프로젝트에 포함된 Gradle Wrapper

## 빌드와 테스트

```bash
./gradlew build
```

애플리케이션 실행:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :apps:api-server:bootRun
```

설정은 환경변수로 주입합니다. 안전한 로컬 예시는 `.env.example`을 참고하되 실제 Secret은
저장소에 Commit하지 않습니다.

## 개발 환경 Endpoint

- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `/swagger-ui.html`
- Health: `GET /actuator/health`
- Prometheus Metric: `GET /actuator/prometheus`

운영 Profile에서는 OpenAPI와 Swagger UI가 비활성화됩니다. Actuator는 기본적으로
`127.0.0.1:8081`의 별도 관리 포트에만 바인딩되며 Health와 Prometheus Endpoint만 노출합니다.
API 오류 응답에는 안정적인 오류 코드와 `traceId`만 포함하고 내부 예외 상세는 노출하지 않습니다.

## 인증 설정

사용자와 Refresh Session은 PostgreSQL에 저장하며 Flyway가 스키마를 관리합니다. Access Token은
15분 기본 만료의 JWT이고, Refresh Token은 HttpOnly·SameSite=Strict Cookie로 전달됩니다. 저장소에는
Refresh Token 원문 대신 SHA-256 해시만 저장하며 갱신할 때마다 회전합니다. 운영 환경에서는
`REFRESH_COOKIE_SECURE=true`와 HTTPS를 사용해야 합니다. `JWT_SECRET_BASE64`는 최소 32바이트의 무작위
값을 Base64로 인코딩해 실행 환경에서만 주입해야 하며 누락 시 애플리케이션은 시작하지 않습니다.

현재 CSRF 정책은 `/api/v1`가 Authorization Bearer Token을 사용한다는 전제에서 해당 경로의 CSRF
Token 검사를 제외합니다. Refresh Cookie는 인증 경로에만 한정하고 `SameSite=Strict`를 적용합니다.
CORS 허용 Origin은 `CORS_ALLOWED_ORIGINS`로 명시하며 와일드카드를 사용하지 않습니다.

## 모듈 구조

- `apps/api-server`: 유일한 Spring Boot 실행 애플리케이션
- `modules/*`: Identity, Account, Order, Execution, Portfolio, Ledger, Market, Outbox 도메인
- `platform/*`: 오류, Web, 보안, 영속성, 메시징, 관측 기술 기반
- `testing/test-support`: 공용 테스트 지원 라이브러리
