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

## 모듈 구조

- `apps/api-server`: 유일한 Spring Boot 실행 애플리케이션
- `modules/*`: Identity, Account, Order, Execution, Portfolio, Ledger, Market, Outbox 도메인
- `platform/*`: 오류, Web, 보안, 영속성, 메시징, 관측 기술 기반
- `testing/test-support`: 공용 테스트 지원 라이브러리
