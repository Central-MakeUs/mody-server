# LLD-0030: DB 마이그레이션 형상관리

> Low-Level Design. 이 문서는 Flyway 기반 DB 마이그레이션 도입과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #96 |
| 관련 ADR | ADR-0021 |
| 작성자 | Codex |
| 작성일 | 2026-07-12 |

## 1. 목적 / 배경

DB 스키마 변경을 수동 ALTER 또는 JPA `ddl-auto`에만 맡기면 환경별 스키마가 달라질 수 있다.
Flyway를 도입해 배포 가능한 DDL 변경을 코드와 함께 관리한다.

## 2. 범위

### In scope

- Flyway 의존성 추가.
- 환경별 Flyway 활성화 설정.
- `mody_group.code` 컬럼 길이 변경 migration 추가.
- migration 운영 규칙 문서화.

### Out of scope

- 전체 초기 스키마를 `V1__init.sql`로 역생성.
- 운영 DB rollback 자동화.
- 테스트 컨테이너 기반 migration 검증.

## 3. 설정

공통 설정:

- `spring.flyway.baseline-on-migrate=true`
- `spring.flyway.baseline-version=1`
- `spring.flyway.locations=classpath:db/migration`
- `spring.flyway.validate-on-migrate=true`

프로필별 기본값:

- `local`: `FLYWAY_ENABLED` 기본값 `false`
- `dev`: `FLYWAY_ENABLED` 기본값 `true`
- `prod`: `FLYWAY_ENABLED` 기본값 `true`
- `test`: `spring.flyway.enabled=false`

## 4. 마이그레이션 파일

```text
src/main/resources/db/migration/V2__alter_group_code_length.sql
```

처리 내용:

1. 현재 DB의 `mody_group.code` 컬럼 길이를 조회한다.
2. 테이블 또는 컬럼이 없으면 no-op 처리한다.
3. 컬럼 길이가 8보다 작으면 `VARCHAR(8) NOT NULL`로 변경한다.
4. 이미 8 이상이면 no-op 처리한다.

## 5. 처리 흐름

dev/prod 기존 DB:

1. 애플리케이션 시작.
2. Flyway가 non-empty schema를 baseline version `1`로 기록한다.
3. `V2__alter_group_code_length.sql`을 실행한다.
4. Hibernate가 이후 `ddl-auto` 설정에 따라 검증 또는 보조 갱신을 수행한다.

local:

1. 기본값으로 Flyway를 실행하지 않는다.
2. 기존 개발 흐름처럼 JPA `ddl-auto=update`가 스키마를 맞춘다.
3. migration 동작 확인이 필요하면 `FLYWAY_ENABLED=true`로 직접 켠다.

## 6. 운영 규칙

- 한 번 원격에 반영된 migration 파일은 수정하지 않는다.
- 보정이 필요하면 다음 번호의 migration 파일을 새로 만든다.
- migration 파일명은 `V{번호}__{동사}_{대상}_{변경}.sql` 형태를 사용한다.
- DB 스키마 변경 PR에는 엔티티 변경, migration 파일, 관련 문서를 함께 포함한다.
- 운영 DB에 직접 수동 DDL을 실행한 경우 후속 migration 또는 문서로 이력을 남긴다.

## 7. 인수조건 (Acceptance Criteria)

- [x] Flyway 의존성이 추가된다.
- [x] dev/prod에서 Flyway가 기본 활성화된다.
- [x] local/test는 기본 개발/테스트 흐름을 깨지 않는다.
- [x] 그룹 코드 컬럼 길이 변경 migration이 추가된다.
- [x] 관련 ADR과 LLD가 작성된다.
- [x] `./gradlew test`가 통과한다.

## 8. 영향 범위 / 마이그레이션

- `dev`, `prod` 기동 시 Flyway가 DB migration을 먼저 수행한다.
- `mody_group.code` 컬럼은 `VARCHAR(8) NOT NULL`로 변경된다.
- 기존 그룹 코드 데이터는 6자리 이하였으므로 데이터 손실은 없다.

## 9. 미결정 사항 (Open Questions)

- 전체 초기 스키마를 별도 `V1__init.sql`로 역생성할지 여부.
- migration 전용 CI 검증에 Testcontainers를 도입할지 여부.

## 10. 참고

- `docs/adr/ADR-0021-database-migration-versioning.md`
- `src/main/resources/db/migration/V2__alter_group_code_length.sql`
