# LLD-0006: ID 생성기 기반

> Low-Level Design. 이 문서는 애플리케이션 레벨 ID 생성기 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #19 |
| 관련 ADR | ADR-0003 |
| 작성자 | msk226 |
| 작성일 | 2026-06-27 |

## 1. 목적 / 배경

- 엔티티 PK를 DB auto increment에 의존하지 않고 애플리케이션에서 생성한다.
- 회원, 소셜 계정, refresh token 등 여러 도메인에서 같은 ID 생성 방식을 재사용한다.
- 숫자 자릿수를 줄이기 위해 2026-01-01 UTC 이후의 초 단위 timestamp를 사용한다.

## 2. 범위

### In scope

- `IdGenerator` 포트 추가.
- Snowflake 계열 `Long` ID 생성 구현체 추가.
- `id.epoch-second`, `id.node-id` 설정 추가.
- ID 생성 단위 테스트 추가.

### Out of scope

- 기존 엔티티 생성 로직 전체에 ID 생성기를 일괄 적용.
- 운영 인스턴스별 node id 할당 자동화.

## 3. 내부 인터페이스

```java
interface IdGenerator {
    Long nextId();
}
```

## 4. 처리 규칙

- ID는 `(timestamp - epoch)`, `nodeId`, `sequence`를 조합한다.
- timestamp는 초 단위로 계산한다.
- node id는 8-bit 범위인 `0..255`만 허용한다.
- 같은 초 안의 중복은 10-bit sequence로 방지한다.
- 같은 노드에서 초당 1024개를 초과하면 다음 초까지 대기한다.
- clock rollback이 감지되면 예외를 던진다.

## 5. 인수조건 (Acceptance Criteria)

- [x] `IdGenerator.nextId()`로 양수 `Long` ID를 생성한다.
- [x] 연속 호출 시 중복 없는 ID를 생성한다.
- [x] 같은 초 안에서도 sequence로 중복을 방지한다.
- [x] 허용 범위를 벗어난 node id는 애플리케이션 시작 시 실패한다.
- [x] `./gradlew build`가 통과한다.

## 6. 영향 범위 / 마이그레이션

- `common/id` 패키지에 ID 생성 포트와 구현체가 추가된다.
- `application.yaml`, test `application.yaml`에 ID 생성 설정이 추가된다.
- ADR-0003의 Snowflake 계열 ID 결정 내용이 구현 기준으로 갱신된다.

## 7. 미결정 사항 (Open Questions)

> 결정되지 않은 항목.

- [ ] 운영 환경에서 인스턴스별 `ID_NODE_ID`를 어떤 방식으로 할당할지.
- [ ] 초당 1024개 생성 제한이 실제 트래픽에서 충분한지.
