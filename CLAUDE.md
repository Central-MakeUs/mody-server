# CLAUDE.md

이 문서는 mody-server에서 작업하는 Claude Code에게 가이드를 제공한다.

## 프로젝트 개요

mody — Central-MakeUs(CMC) 백엔드. Kotlin / Spring Boot 기반 API 서버. 패키지 루트는 `cmc.mody`. 현재 단일 모듈.

## 기술 스택 (build.gradle 기준 사실)

> 안정 스택으로 확정 (ADR-0002). Spring Boot 4 + 안정 detekt 공존 불가라 spot-kotlin과 동일한 검증된 3.x 스택을 사용한다.

- **Kotlin**: 2.0.21
- **Java**: 21 (toolchain)
- **Spring Boot**: 3.4.1
- **영속성**: Spring Data JPA / Hibernate, MySQL (`mysql-connector-j`)
- **직렬화**: Jackson 2 (`com.fasterxml.jackson.module:jackson-module-kotlin`)
- **API 문서**: springdoc-openapi 2.7.0 (`springdoc-openapi-starter-webmvc-ui`) — Swagger UI
- **린트**: ktlint 1.5.0 + detekt 1.23.8
- **Lombok**: build.gradle에 포함됨 (compileOnly + annotationProcessor)
- **빌드**: Gradle (Groovy DSL — `build.gradle`)
- **테스트**: JUnit 5 (`kotlin-test-junit5`)

## 빌드 / 실행 / 테스트

```bash
./gradlew build          # 빌드
./gradlew test           # 전체 테스트
./gradlew test --tests "cmc.mody.SomeTest"   # 단일 테스트
./gradlew bootRun        # 로컬 실행
```

## 디렉토리 구조

```
src/main/kotlin/cmc/mody/         애플리케이션 코드
src/main/resources/application.yaml
docs/adr/                         ADR (Architecture Decision Records)
docs/lld/                         LLD (Low-Level Design) — PR 오라클
docs/templates/                   ADR/LLD 템플릿
.claude/skills/                   commit, pr 스킬
```

## 개발 워크플로우 (spec-driven / 하네스 엔지니어링)

기능은 다음 순서로 완성한다:

```
ADR (왜 이렇게 결정했나) ─┐
                         ├─→ 구현 ─→ PR (본문 = LLD 기반) ─→ 리뷰 ─→ main 머지(자동 배포)
LLD (무엇을/어떻게)      ─┘
```

- **작업 단위 = GitHub Issue** (지라 미사용). Issue ↔ LLD ↔ branch ↔ PR ↔ commit 으로 추적한다.
- **LLD에는 반드시 `## 미결정 사항(Open Questions)` 섹션**을 둔다. 이 섹션이 PR에서 빈칸으로 처리되는 근거다.

## ⚠️ 최우선 규칙 — 임의 의사결정 금지

- LLD / ADR / 코드에 명시되지 않은 결정을 추측으로 채우지 않는다.
- 결정되지 않은 부분은 **빈칸 + `⚠️ 확인 필요`**로 남기고 사용자에게 확인받는다.
- 특히 **PR 본문은 LLD를 오라클로만 사용**한다. (`.claude/skills/pr` 참고)

## Git 워크플로우 (Trunk-Based Development)

- **main에 직접 머지 → 머지 시 자동 배포.** PR은 작고 안전하게 유지한다.
- **브랜치**: `feature/{issue-number}` (예: `feature/12`). base는 항상 `main`.
- **커밋**: Conventional Commits, 타입 영문 / 본문 한글. (`.claude/skills/commit`)
- **PR**: `.claude/skills/pr` 스킬로 생성, 본문은 LLD 기반.

## 코드 스타일 / 린트 (spot-kotlin과 동일)

- 4-space 들여쓰기, 최대 120자, UTF-8, LF, 파일 끝 newline 필수.
- **ktlint**(`ktlint_official`)가 소스 오브 트루스, **detekt**가 보조 (`config/detekt/detekt.yml`).
- `./gradlew ktlintCheck` / `ktlintFormat` / `detekt`.
- `.editorconfig`, `config/detekt/detekt.yml`은 spot-kotlin에서 가져와 설정 완료.

## 도메인 / 엔티티 패턴 (spot-kotlin과 동일)

- private 생성자 + companion object 팩토리(`of(...)`), 팩토리에서 검증 수행.
- 가변 속성은 `var ... private set`, 진짜 불변은 `val`.
- soft delete: `@SQLDelete` + `@SQLRestriction`.
- 공통 감사 필드는 `BaseEntity` 상속.

## 패키지 / 레이어링

- 루트 패키지 `cmc.mody`. feature 단위로 패키지를 구성하고 DDD 지향.
- **모놀리식 단일 모듈**로 시작 (ADR-0001). batch 독립 배포가 필요해지면 `core`+`api`+`batch`로 추출.
- **Lombok 사용** (build.gradle에 설정됨).

## CI/CD (GitHub Actions)

- GitHub Actions 사용. spot의 `.github/workflows`(build → Docker → ssh deploy) 패턴을 참고한다.
- TBD에 맞춰 트리거는 **main 기준**: PR → 테스트, main 머지 → build + 자동 배포.
- 인프라 상세는 아래 미확정.

## 공통 응답 / 예외 처리

- 응답은 `cmc.mody.common.api.ApiResponse<T>` 로 감싼다 (`isSuccess`, `code`, `message`, `result`). `ApiResponse.ok/created/failure(...)` 사용.
- 상태 코드: `SuccessStatus` / `ErrorStatus` (`BaseCode` 구현). **공통 코드만 존재**하며, 도메인별 에러 코드는 기능 추가 시 `ErrorStatus`에 함께 정의한다.
- 예외는 `GeneralException(status)` 로 던지고 `GlobalExceptionHandler`가 `ApiResponse.failure`로 변환한다.

## 미확정 / 확인 필요

- [ ] **배포 인프라 상세**: 배포 대상 서버/호스팅, Docker 레지스트리, 필요한 GitHub Secrets.
- [ ] **테스트 전략 / 커버리지 기준** (현재 `contextLoads` 컨텍스트 테스트는 DB 설정 필요).
