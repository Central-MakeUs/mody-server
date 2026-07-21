# LLD-0013: 기록 피드 조회 API

> Low-Level Design. 이 문서는 기록 피드 조회 API 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #42 |
| 관련 ADR | ADR-0007, ADR-0014 |
| 작성자 | Codex |
| 작성일 | 2026-07-01 |

## 1. 목적 / 배경

기록 업로드 API로 저장된 식사/운동 기록을
그룹 화면에서 주간 활동 여부와 날짜별 피드로 조회한다.

## 2. 범위

### In scope

- 주간 그룹 전체 기록 존재 여부 조회.
- 날짜별 식사/운동 기록 커서 페이징 조회.
- 날짜별 기록 조회에서 작성자별 현재 연속 기록 일수 응답.
- 그룹 참여 여부 검증.
- 삭제 처리된 기록과 그룹에서 나간 회원의 기록 제외.
- Swagger 성공/예외 응답 명세 보강.

### Out of scope

- 기록 상세 조회.
- 기록 댓글 조회/작성.
- 이미지 read signed URL 발급.
- 그룹 나가기 시 기존 기록 삭제 처리.

## 3. 인터페이스 / API

모든 구현 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
GET /api/v1/groups/{groupId}/activities/calendar?baseDate=2026-07-13
GET /api/v1/groups/{groupId}/records?date=2026-07-01&cursor=100&size=20
```

활동 달력은 `baseDate`가 속한 주의 일요일~토요일 7일을 반환한다.
각 날짜는 그룹 전체 기록이 하나라도 있으면 `hasRecord=true`다.

기록 목록 응답은 최신순이며 `nextCursor`는 마지막 record id다.
다음 페이지는 `cursor={nextCursor}`로 요청한다.
각 기록에는 해당 작성자의 `recordingStreakDays`를 함께 응답한다.

## 4. 데이터 모델

- `activity_record`
  - `group_id`, `uploaded_at`, `id` 기준으로 조회한다.
  - `deleted_at is null`인 기록만 조회한다.
- `group_member`
  - 조회 요청자가 `JOINED` 상태인지 확인한다.
  - 작성자도 현재 `JOINED` 상태인 경우만 피드에 포함한다.
  - 표시 닉네임과 프로필 이미지는 `display_nickname`, `display_profile_image_key`를 사용한다.

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

1. `@CurrentMember`가 access token에서 회원 id를 추출한다.
2. 활성 회원을 조회한다.
3. 요청자가 해당 그룹의 `JOINED` 회원인지 확인한다.
4. 달력 조회는 기준 날짜가 속한 주의 일요일 이상, 다음 주 일요일 미만의 기록을 조회한다.
   응답은 일요일부터 토요일까지 항상 7개 날짜를 포함한다.
5. 피드 조회는 날짜 시작 시각 이상, 다음 날짜 시작 시각 미만의 기록을 조회한다.
   정렬은 `id desc`를 사용한다.
6. 커서가 있으면 `record.id < cursor` 조건을 추가한다.
7. `size + 1`개를 조회해 `hasNext`와 `nextCursor`를 계산한다.
8. 응답에 포함된 작성자별로 조회 기준일 이전 기록 날짜를 확인해 현재 연속 기록 일수를 계산한다.

## 6. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 회원 없음: `MEMBER302`.
- 그룹 없음: `GROUP302`.
- 그룹 참여 정보 없음: `GROUP306`.
- 날짜 파라미터 형식 오류: `RECORD301`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 그룹 참여 회원이 주간 활동 여부를 조회할 수 있다.
- [x] 주간 활동 여부는 그룹 전체 기록 기준으로 계산된다.
- [x] 그룹 참여 회원이 날짜별 기록 목록을 커서 기반으로 조회할 수 있다.
- [x] 날짜별 기록 목록에서 작성자별 현재 연속 기록 일수를 확인할 수 있다.
- [x] 삭제 처리된 기록과 현재 그룹원이 아닌 작성자의 기록은 조회되지 않는다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.
- [x] 서비스 단위 테스트가 주요 분기를 검증한다.

## 8. 영향 범위 / 마이그레이션

- `ActivityRecordService`에 조회 기능이 추가된다.
- 기록 목록 응답에 `recordingStreakDays`가 추가된다.
- `ActivityRecordRepository`에 피드 조회용 JPQL query가 추가된다.
- 기존 기록 상세/댓글 API stub은 이번 범위에서 유지한다.

## 9. 미결정 사항 (Open Questions)

- 이미지 조회 URL은 현재 `upload.base-url + imageKey`로 응답한다.
- 이미지 관심 영역 좌표가 저장된 기록은 `imageCropRegion(x, y, width, height)`를 함께 응답한다.
  read signed URL이 필요하면 후속 이슈에서 분리한다.
- 날짜 기준은 서버 로컬 날짜로 처리한다.
  별도 사용자 타임존 정책이 생기면 API 스펙을 조정한다.

## 10. 참고

- `docs/adr/ADR-0007-cursor-pagination-strategy.md`
- `docs/adr/ADR-0014-record-streak-calculation.md`
- `docs/lld/LLD-0012-activity-record-create-api.md`
