# ADR-0002: Spring Boot 3.4.x 안정 스택 채택 (Boot 4 미사용)

> Architecture Decision Record. 하나의 중요한 의사결정과 그 이유를 기록한다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-06-23 |
| 관련 | - |

## 맥락 (Context)

Spring Initializr가 mody를 **Spring Boot 4.1.0 + Kotlin 2.3.21**로 생성했다. 이 조합에서 다음 문제가 드러났다:

- Spring Boot 4 / Spring Framework 7 바이너리는 **Kotlin 메타데이터 2.2.0**으로 컴파일되어 **Kotlin 2.2+ 컴파일러를 강제**한다 (2.0.21로는 `runApplication`조차 해석 불가).
- 그러나 **안정판 detekt(1.23.8)는 Kotlin 2.0.21까지만** 지원한다. Kotlin 2.3을 지원하는 detekt는 `2.0.0-alpha`(알파)뿐이다.
- 즉 **Spring Boot 4와 안정판 detekt는 공존할 수 없다.**

프로젝트 성격(CMC, 안정성 우선, "최신 불필요")과 레퍼런스 프로젝트(spot-kotlin = Boot 3.4.1)를 고려했다.

## 결정 (Decision)

**Spring Boot 3.4.1 + Kotlin 2.0.21 안정 스택**을 채택한다. spot-kotlin과 동일한 검증된 구성이며 alpha 의존성이 없다.

- Spring Boot 3.4.1, Kotlin 2.0.21, Java 21
- Jackson 2 (`com.fasterxml.jackson.module:jackson-module-kotlin`)
- springdoc-openapi 2.7.0 (Swagger)
- ktlint 1.5.0 + detekt 1.23.8

Boot 4 전용 설정(분리형 test starter, `spring-boot-starter-webmvc`, Jackson 3, `-Xannotation-default-target` 플래그)은 3.x에 맞게 수정했다.

## 고려한 대안 (Considered Options)

1. **Boot 3.4.x 안정 스택** (채택) — 전부 안정판, alpha 없음, 레퍼런스와 동일. Boot 4 신기능은 포기.
2. **Boot 4.1.0 유지 + detekt 2.0.0-alpha** — 신기능 유지하나 정적분석이 alpha.
3. **Boot 4.1.0 유지 + detekt 보류** — alpha는 없으나 정적분석 커버리지 없음.

## 결과 (Consequences)

### 긍정
- 모든 의존성이 안정판. 빌드/린트가 재현 가능하고 생태계 호환이 검증됨.
- spot-kotlin과 동일 스택이라 패턴·설정 재사용이 쉽다.

### 부정 / 트레이드오프
- Spring Boot 4의 신기능(API 버저닝 내장, Jackson 3, JSpecify 널 안정성, 선언적 HTTP 클라이언트 등)을 쓰지 못한다.
- 추후 Boot 4로 올리려면 생태계(특히 detekt 안정판 Kotlin 2.3 지원)가 성숙한 뒤 마이그레이션이 필요하다.

## 후속 / 미결정
- [ ] detekt 안정판이 Kotlin 2.3+를 지원하면 Boot 4 상향을 재검토.
