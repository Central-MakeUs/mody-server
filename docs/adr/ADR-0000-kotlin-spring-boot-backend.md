# ADR-0000: Kotlin 기반 Spring Boot 백엔드 채택

> Architecture Decision Record. 하나의 중요한 의사결정과 그 이유를 기록한다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-06-23 |
| 관련 | ADR-0002 |

## 맥락 (Context)

mody 백엔드는 초기 개발 속도와 유지보수성을 모두 고려해야 한다. API 서버는 DTO, 값 객체, 도메인 생성 규칙, 예외 처리, 테스트 fixture처럼 반복 코드가 많이 생긴다. 또한 AI 도구와 함께 작업하는 비중이 높으므로 코드량이 적고 의도가 잘 드러나는 언어가 유리하다.

## 결정 (Decision)

mody 백엔드는 Java 대신 Kotlin 기반 Spring Boot로 개발한다.

- Kotlin은 Spring Boot에서 공식적으로 지원되는 JVM 언어다.
- Java 생태계의 Spring, JPA, Gradle, JUnit, JWT 라이브러리를 그대로 사용할 수 있다.
- null-safety로 DTO, JPA nullable 컬럼, OAuth 응답 처리에서 NPE 가능성을 줄인다.
- `data class`, default parameter, named argument로 요청/응답 DTO와 값 객체의 보일러플레이트를 줄인다.
- companion object factory, private constructor, enum, sealed type 등을 통해 도메인 생성 규칙과 상태 표현을 코드에 명확히 담을 수 있다.
- 문법이 간결해 AI가 코드를 읽고 수정할 때 변경 범위 파악이 쉽고 반복 코드 생성 실수가 줄어든다.
- 테스트 fixture와 assertion 코드가 짧아져 테스트 가독성이 좋아진다.

## 고려한 대안 (Considered Options)

1. **Kotlin + Spring Boot** (채택) — JVM 생태계를 유지하면서 간결한 문법, null-safety, 낮은 보일러플레이트를 얻는다.
2. **Java + Spring Boot** — 팀과 생태계 측면에서 가장 보편적이나 DTO/값 객체/테스트 코드의 반복이 많다.
3. **Node.js/TypeScript** — 프론트엔드와 언어를 공유할 수 있지만, 현재 백엔드 안정 스택과 JPA 기반 설계에는 JVM이 더 적합하다.

## 결과 (Consequences)

### 긍정
- DTO, 값 객체, 테스트 코드가 짧고 명확해진다.
- null-safety로 런타임 오류 가능성을 줄일 수 있다.
- Java 라이브러리와 운영 생태계를 그대로 활용한다.
- AI 협업 시 생성/수정해야 하는 코드량이 줄어든다.

### 부정 / 트레이드오프
- JPA 엔티티는 Kotlin의 final class, no-arg constructor, lazy loading 특성과 충돌할 수 있다.
- Kotlin 문법과 컨벤션에 대한 팀 숙련도가 필요하다.
- 일부 Spring/JPA 예제와 문서는 Java 기준이라 Kotlin 변환 판단이 필요하다.

## 후속 / 미결정

- Kotlin JPA 제약은 `kotlin("plugin.jpa")`, `kotlin("plugin.spring")`와 엔티티 작성 규칙으로 관리한다.
- 엔티티 생성은 private constructor와 companion object factory를 기본 패턴으로 둘지 ERD 이후 도메인 구현에서 확정한다.
