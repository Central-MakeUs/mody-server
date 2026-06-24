# ADR-0002: Spring Boot 3.4.x 안정 스택 채택 (Boot 4 미사용)

> Architecture Decision Record. 하나의 중요한 의사결정과 그 이유를 기록한다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-06-23 |
| 관련 | - |

## 맥락 (Context)

mody는 신규 CMC 백엔드 프로젝트이며 안정성과 예측 가능한 운영을 우선한다. 최신 major 버전보다 검증된 Spring Boot 3.x 라인, Java 21, JPA, MySQL 조합을 사용해 초기 개발과 배포 리스크를 줄인다.

## 결정 (Decision)

**Spring Boot 3.4.1 + Java 21 안정 스택**을 채택한다. alpha 의존성 없이 Spring/JPA 생태계의 일반적인 구성을 사용한다.

- Spring Boot 3.4.1, Java 21
- Jackson 2
- springdoc-openapi 2.7.0 (Swagger)

Boot 4 전용 설정은 도입하지 않는다.

## 고려한 대안 (Considered Options)

1. **Boot 3.4.x 안정 스택** (채택) — 전부 안정판이며 운영 사례가 많다. Boot 4 신기능은 포기한다.
2. **Boot 4.x 조기 도입** — 최신 기능을 사용할 수 있으나 초기 프로젝트에 불필요한 생태계 변동성을 만든다.

## 결과 (Consequences)

### 긍정
- 모든 주요 의존성이 안정판이다.
- Spring/JPA 예제와 운영 사례를 그대로 활용하기 쉽다.
- 빌드 구성이 단순하다.

### 부정 / 트레이드오프
- Spring Boot 4의 신기능(API 버저닝 내장, Jackson 3, JSpecify 널 안정성, 선언적 HTTP 클라이언트 등)을 쓰지 못한다.
- 추후 Boot 4로 올리려면 별도 마이그레이션이 필요하다.

## 후속 / 미결정
- [ ] Spring Boot 4 생태계가 충분히 안정화되면 상향을 재검토.
