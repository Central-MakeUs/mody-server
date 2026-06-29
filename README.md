# 친구들과 함께하는 다이어트 습관, 모디 mody

모디(mody)는 친구들과 함께 다이어트 습관을 만들고 지속할 수 있도록 돕는 서비스입니다.
이 저장소는 모디의 Java/Spring Boot 기반 백엔드 API 서버입니다.

## Tech Stack

- Java 21
- Spring Boot 3.4.1
- Spring Data JPA / MySQL
- JUnit 5
- Gradle

## Getting Started

```bash
./gradlew build
./gradlew test
./gradlew bootRun
```

로컬 실행은 기본적으로 `local` profile을 사용하며 MySQL 연결이 필요합니다.
DB 설정은 환경변수로 재정의할 수 있습니다.

```bash
DB_URL=jdbc:mysql://localhost:3306/mody
DB_USERNAME=root
DB_PASSWORD=...
```

## Verification

```bash
./gradlew build
```

## Development Process

이 프로젝트는 하네스 엔지니어링 방식으로 기능 개발 과정을 관리합니다.
생성된 코드 자체보다, 코드를 만들고 검증하는 주변 장치를 먼저 고정해 변경 품질을 유지하는 접근입니다.

1. 구현할 기능의 범위와 정책을 Issue로 분리합니다.
2. 기능 단위 설계는 LLD로 먼저 작성합니다.
3. 기술적 의사결정은 ADR로 남겨 선택지와 트레이드오프를 기록합니다.
4. API 계약과 예외 케이스는 Swagger 문서 테스트로 고정합니다.
5. 테스트 시나리오를 기준으로 코드를 생성하고 구현을 보완합니다.
6. 사람이 설계, 테스트, diff를 검토한 뒤 PR을 생성합니다.
7. CI에서 빌드와 테스트를 통과한 변경만 배포 대상으로 봅니다.
8. main 반영 후 dev 환경에 배포하고 헬스 체크로 실행 상태를 확인합니다.

## Documentation

- Architecture decisions: [docs/adr](docs/adr)
- Low-level designs: [docs/lld](docs/lld)
