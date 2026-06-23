# 친구들과 함께하는 다이어트 습관, 모디 mody

모디(mody)는 친구들과 함께 다이어트 습관을 만들고 지속할 수 있도록 돕는 서비스입니다.
이 저장소는 모디의 Kotlin/Spring Boot 기반 백엔드 API 서버입니다.

## Tech Stack

- Kotlin 2.0.21
- Java 21
- Spring Boot 3.4.1
- Spring Data JPA / MySQL
- JUnit 5
- ktlint / detekt
- Gradle Kotlin DSL

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
./gradlew ktlintCheck
./gradlew detekt
./gradlew test
```

## Documentation

- Architecture decisions: [docs/adr](docs/adr)
- Low-level designs: [docs/lld](docs/lld)
