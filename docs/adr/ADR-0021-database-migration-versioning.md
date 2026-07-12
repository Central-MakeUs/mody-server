# ADR-0021: Flyway 기반 DB 마이그레이션 관리

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-07-12 |
| 관련 이슈 | #96 |

## 1. 맥락

현재 스키마 변경은 JPA `ddl-auto` 또는 수동 DDL에 의존한다.
컬럼 길이 변경, 인덱스 추가, 신규 테이블 생성이 반복되면 dev/prod/local 간 스키마 차이가 생길 수 있다.
특히 `mody_group.code`처럼 엔티티 정책은 8자리인데 실제 DB 컬럼이 6자리로 남는 경우가 발생할 수 있다.

## 2. 결정

- DB 마이그레이션 도구로 Flyway를 도입한다.
- migration 파일은 `src/main/resources/db/migration` 아래에 둔다.
- 기존 운영 DB는 이미 테이블이 존재하므로 `baseline-on-migrate=true`, `baseline-version=1`을 사용한다.
- 실제 변경 migration은 `V2__...sql`부터 작성한다.
- `local`은 기본적으로 Flyway를 비활성화하고 JPA `ddl-auto=update` 흐름을 유지한다.
- `dev`, `prod`는 기본적으로 Flyway를 활성화한다.
- `prod`는 JPA `ddl-auto=validate`를 유지해 migration 이후 엔티티와 DB 스키마 불일치를 감지한다.

## 3. 결과

긍정적 효과:

- 스키마 변경 이력이 코드로 남아 환경 간 차이를 줄일 수 있다.
- dev/prod 배포 시 필요한 DDL을 애플리케이션 시작 단계에서 일관되게 적용할 수 있다.
- 수동 ALTER 누락으로 발생하는 런타임 장애 가능성을 줄인다.

부정적 효과 / 리스크:

- migration 파일은 한 번 배포되면 수정하지 않고 새 버전으로 보정해야 한다.
- 잘못된 DDL이 배포되면 애플리케이션 기동이 실패할 수 있다.
- 기존 DB에 최초 도입할 때 baseline 설정을 잘못 잡으면 이미 존재하는 스키마와 충돌할 수 있다.

## 4. 재검토 기준

- prod DB가 여러 인스턴스 또는 여러 리전으로 확장될 때.
- migration 검증을 별도 CI 단계에서 수행해야 할 때.
- rollback/down migration 전략이 필요해질 때.
