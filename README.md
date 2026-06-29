# mody

> 친구들과 함께하는 다이어트 습관, 모디

모디는 친구들과 함께 식사, 운동, 체중 기록을 공유하며 다이어트 습관을 이어갈 수 있도록 돕는 서비스입니다.
이 저장소는 모디의 Java/Spring Boot 기반 백엔드 API 서버입니다.

## How We Build

이 프로젝트는 **하네스 엔지니어링** 방식으로 개발합니다.
설계와 검증 장치를 먼저 고정하고, 그 기준에 맞춰 구현 품질을 관리합니다.

```text
Team Discussion
  -> ADR
  -> LLD
  -> Test Scenario
  -> Code Generation
  -> Human Review
  -> PR / CI / Deploy
```

팀 내부에서 논의된 기술적 선택은 ADR로 남깁니다.
ADR에는 선택한 방법뿐 아니라 고려한 대안, 트레이드오프, 후속 리스크를 함께 기록합니다.

ADR이 방향을 정하면 LLD에서 실제 구현 단위를 설계합니다.
API 요청/응답, 도메인 규칙, 예외 코드, 인증 조건, 저장 방식처럼 코드로 옮겨질 세부사항을 먼저 정리합니다.

LLD에서 예상되는 엣지 케이스는 테스트 시나리오로 명시합니다.
이 시나리오를 기준으로 코드를 생성하고, 사람이 diff와 테스트를 검토한 뒤 PR로 병합합니다.

## Technical Baseline

| Category | Stack |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Persistence | Spring Data JPA, MySQL |
| API Docs | Swagger, Spring REST Docs |
| Test | JUnit 5, MockMvc |
| Build | Gradle |
| Deploy | GitHub Actions, Docker, GCP Compute Engine |

## Getting Started

로컬 실행은 기본적으로 `local` profile을 사용하며 MySQL 연결이 필요합니다.
DB 설정은 환경변수로 재정의할 수 있습니다.

```bash
./gradlew bootRun
```

```bash
DB_URL=jdbc:mysql://localhost:3306/mody
DB_USERNAME=root
DB_PASSWORD=...
```

## Verification

```bash
./gradlew build
./gradlew test
```

## Documentation

- [ADR](docs/adr): 팀 논의 기반의 기술적 의사결정 기록
- [LLD](docs/lld): 기능 단위 구현 설계와 정책 정리
