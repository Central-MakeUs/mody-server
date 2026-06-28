# ADR-0003: DB 및 엔티티 설계 기준 채택

> Architecture Decision Record. 하나의 중요한 의사결정과 그 이유를 기록한다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-06-23 |
| 관련 | LLD-0003 |

## 맥락 (Context)

ERD 설계 전에 ID 생성, 삭제 정책, 네이밍, 참조 방식, 토큰 저장 방식을 먼저 고정해야 한다. 이 기준이 없으면 엔티티 구현, 인덱스 설계, API 응답, 인증 흐름이 기능마다 달라질 수 있다.

## 결정 (Decision)

- PK는 `Long` 타입의 애플리케이션 레벨 Snowflake 계열 ID를 사용한다.
- ID는 DB auto increment에 의존하지 않고 `IdGenerator.nextId()` 포트로 생성한다.
- Snowflake 계열 ID는 2026-01-01 UTC epoch, 초 단위 timestamp, 8-bit node, 10-bit sequence를 사용한다.
- 삭제 정책은 soft delete를 기본으로 한다.
- soft delete 대상 엔티티는 `status = ACTIVE/INACTIVE`를 사용하고, JPA 엔티티에는 `@SQLDelete`와 `@SQLRestriction` 적용을 기본으로 한다.
- unique 값 재사용이 필요한 엔티티는 삭제 시 unique 컬럼 값에 삭제 suffix를 붙인다.
- 테이블/컬럼 네이밍은 `snake_case`를 사용한다.
- 테이블명은 명확한 도메인 단수명 또는 조인 목적이 드러나는 이름을 사용한다. 예: `member`, `refresh_token`.
- 공통 컬럼은 `id`, `created_at`, `updated_at`, `status`를 기본으로 한다.
- enum은 문자열로 저장한다.
- DB 외래키 제약조건은 사용하지 않는다.
- 엔티티 간 참조는 `member_id`, `group_id` 같은 `Long` id 컬럼으로만 저장한다.
- JPA 엔티티에는 `@ManyToOne`, `@OneToOne`, `@JoinColumn` 기반 연관관계를 두지 않는다.
- 참조 무결성은 애플리케이션 유스케이스와 조회 쿼리에서 검증한다.
- refresh token은 DB에 저장한다. Redis는 초기 설계 범위에서 제외한다.

## 고려한 대안 (Considered Options)

1. **Snowflake 계열 Long ID** (채택) — DB 의존 없이 ID를 먼저 만들 수 있고, 서비스/도메인 로직에서 연관 엔티티 생성이 단순하다. 단, 비트 배분과 clock rollback 대응이 필요하다.
2. **DB auto increment** — 단순하고 운영 부담이 낮지만, 저장 전 ID가 필요한 도메인 흐름과 배치 생성에서 제약이 있다.
3. **UUID** — 분산 생성이 쉽지만 URL/로그/DB 인덱스에서 가독성과 저장 효율이 떨어진다.
4. **refresh token Redis 저장** — 만료 관리가 편하지만 초기 운영 구성과 장애 지점이 늘어난다.
5. **DB 외래키 제약조건 사용** — DB 레벨 무결성 보장이 강하지만, soft delete, 데이터 이관, 테스트 데이터 구성, 초기 기획 변경 대응에서 제약이 커진다.
6. **활성 row 전용 unique 인덱스** — DB 레벨 제약으로 처리할 수 있으나 MySQL에서는 generated column 등 추가 설계가 필요하다.

## 결과 (Consequences)

### 긍정
- ERD와 엔티티 구현 기준이 명확해진다.
- DB 저장 전에도 도메인 객체 ID를 부여할 수 있다.
- soft delete와 공통 감사 필드를 일관되게 적용할 수 있다.
- DB 외래키 제약 없이 초기 ERD 변경과 데이터 이관에 유연하게 대응할 수 있다.
- refresh token 저장 구조가 DB 중심으로 단순해진다.

### 부정 / 트레이드오프
- Snowflake 파라미터를 잘못 줄이면 초당 생성량, 노드 수, 사용 기간 중 하나가 제한된다.
- soft delete는 unique 제약, 조회 조건, 인덱스 설계에 추가 고려가 필요하다.
- 삭제 suffix 방식은 구현이 단순하지만, 삭제 시 도메인별 unique 컬럼 변형 로직이 필요하다.
- 외래키 제약조건을 쓰지 않으므로 잘못된 참조 id가 저장되지 않도록 서비스 레이어 검증과 테스트가 필요하다.
- DB refresh token 저장은 즉시 만료/블랙리스트 처리 시 DB 조회가 필요하다.

## 후속 / 미결정

- [x] Snowflake epoch, timestamp bits, node bits, sequence bits 최종 확정.
- [ ] 자릿수 축소 목표치와 허용 가능한 최대 사용 기간 계산.
- [ ] node id 할당 방식: 환경변수, 단일 노드 고정값, 배포 인스턴스별 값.
- [ ] 삭제 suffix 포맷과 대상 unique 컬럼 목록.
- [ ] 참조 id 유효성 검증 공통 패턴.
- [ ] refresh token rotation, 재사용 감지, 만료 컬럼 설계.
