# LLD-0037: 관리 화면 회원 · 운영 메뉴

| 항목 | 값 |
| --- | --- |
| 상태 | Review |
| Issue | #241 |
| 관련 ADR | - |
| 작성자 | Claude |
| 작성일 | 2026-10-17 |

## 1. 목적 / 배경

운영 서버를 Oracle A1(4 OCPU / 24GB)로 옮겨 관리 화면에 집계 조회를 더해도 부담이 없어졌다.
회원 문의 대응, 신고 처리, 공지, 지표 확인을 DB 직접 조회나 Grafana 없이 관리 화면에서 할 수 있게 한다.
기능은 단계별 PR로 나눠 넣는다. 각 PR이 이 문서의 해당 절을 채운다.

| 단계 | 기능 | 상태 |
| --- | --- | --- |
| 1 | 회원 관리, 작업 이력 | 이 PR |
| 2 | 신고 처리 | 예정 |
| 3 | 공지 푸시 | 예정 |
| 4 | 통계 | 예정 |

## 2. 범위

- 새 화면 `/admin/ops/index.html` 하나에서 경로(`/admin/members`, `/admin/audit` 등)로 탭을 고른다.
  기존 콘솔 스타일을 그대로 쓰고 표·상세 패널 스타일만 `ops/styles.css`에 더한다.
- 로그인은 기존 `/admin` 콘솔에 맡긴다. 세션이 없으면 `/admin?next=<경로>`로 보내고,
  콘솔은 허용된 경로 목록에 있을 때만 로그인 후 돌려보낸다.
- 회원 정지, 데이터 내보내기는 포함하지 않는다.

## 3. 인터페이스 / API

모든 API는 기존 관리자 인증(세션 + CSRF 또는 `X-Admin-Api-Key`)을 따른다.

### 회원

`GET /api/v1/admin/members`

- `keyword`: 닉네임 부분 일치. 숫자만이면 회원 ID 일치도 함께 찾는다. `%`, `_`는 문자로 검색. 최대 100자.
- `state`: `ALL`(기본), `ACTIVE`, `WITHDRAWN`. `loginType`: `KAKAO`, `APPLE`, `GOOGLE`, `ANDROIDTEST`, `IOSTEST`.
- `size`: 기본 30, 최대 100. `cursor`: 가입 시각·ID 내림차순 키셋 커서.
- 항목: `id`(문자열), 닉네임, 프로필 이미지 URL, 가입 경로 목록, 가입·탈퇴 시각, 활성 여부,
  참여 중인 그룹 수, 유효 활동 기록 수, 최근 기록 시각.

`GET /api/v1/admin/members/summary`: 전체, 활성, 탈퇴, 오늘 가입, 최근 7일 가입, 최근 7일 탈퇴(한국시간).

`GET /api/v1/admin/members/{id}`: 기본 정보(생년월일, 목표 체중, 건강 앱 연동, 그룹 온보딩),
가입 경로와 연결 시각, 그룹 이력(최대 50), 활동 수(식사·운동·챌린지 인증·댓글·걸음 일수·누적 걸음),
한 신고·받은 신고 수, 푸시 기기(플랫폼·수신 여부·최근 접속), 최근 기록 12건, 유효 로그인 세션 수. 없으면 404.

토큰 값, FCM 토큰, 소셜 제공자 식별자는 응답에 넣지 않는다.

### 작업 이력

`GET /api/v1/admin/audit-logs?actor=&cursor=&size=50`: 최신순 키셋 페이지.

## 4. 데이터 모델

`V19__add_admin_audit_log.sql`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | Snowflake ID |
| `actor_admin_id` | BIGINT NULL | 관리자 계정 ID. API 키 호출은 NULL |
| `actor_label` | VARCHAR(320) | 관리자 이메일 또는 `machine` |
| `http_method` | VARCHAR(10) | POST, PUT, PATCH, DELETE |
| `request_path` | VARCHAR(500) | 요청 경로(쿼리 문자열 제외, 500자 절단) |
| `status_code` | INT | 응답 상태 |
| `created_at` | DATETIME(6) | 기록 시각 |

인덱스 `(created_at, id)`. 엔티티 `AdminAuditLog`는 스키마 검증(`ddl-auto: validate`)과 테스트용이다.

## 5. 처리 흐름

- `AdminRequestInterceptor.afterCompletion`이 읽기(GET/HEAD/OPTIONS)가 아닌 요청을 기존 로그와 함께
  `AdminAuditService.record`로 저장한다. 저장 실패는 경고 로그만 남기고 원 요청 결과를 바꾸지 않는다.
- 회원 목록은 페이지 1회 + 페이지 회원들의 가입 경로 일괄 조회 1회다. 항목별 집계는 상관 서브쿼리로
  페이지 크기(최대 100) 안에서만 실행한다.
- 화면은 이전 요청 결과가 늦게 와도 새 결과를 덮지 않도록 세대 번호를 확인한다.

## 6. 예외 / 에러 처리

잘못된 상태·가입 경로·커서·크기는 400, 없는 회원은 404(`MEMBER302`).

## 7. 인수조건

- [x] 상태·가입 경로·검색어 필터와 키셋 페이지가 중복 없이 동작한다.
- [x] `%`, `_` 검색이 와일드카드로 해석되지 않는다.
- [x] 상세 응답에 토큰·소셜 식별자가 없다.
- [x] 변경 요청이 작업 이력에 남고, 실행자로 거를 수 있다.
- [x] 운영 MySQL에서 목록·집계 쿼리가 동작한다(읽기 전용 계정으로 확인).

## 8. 영향 범위 / 배포

DB 마이그레이션 V19(새 테이블)를 추가한다. 블루그린 배포 중 이전 버전은 이 테이블을 쓰지 않으므로 하위 호환이다.

## 9. 미결정 사항

- 작업 이력 보관 기간. 지금은 삭제하지 않는다.

## 10. 참고

- [LLD-0035](LLD-0035-admin-console.md), [LLD-0036](LLD-0036-admin-management-renewal.md)
