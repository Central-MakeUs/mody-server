# LLD-0020: 온보딩 및 피드 API 스펙 정리

> Low-Level Design. 이 문서는 온보딩 및 피드 API 스펙 정리 구현과 PR 본문의 **오라클(ground truth)** 이다.

> 최신 기록 정책: 기록 생성/상세/댓글의 현재 구현 기준은 `docs/lld/LLD-0031-record-upload-all-groups.md`를 따른다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #64 |
| 관련 ADR | ADR-0013, ADR-0014, ADR-0015 |
| 작성자 | Codex |
| 작성일 | 2026-07-04 |

## 1. 목적 / 배경

클라이언트 플로우 검토 중 API 스펙의 중복 입력과 응답 누락이 확인되었다.
이번 작업은 새 기능을 크게 늘리기보다, 기존 온보딩/마이페이지/피드/그룹/알림 API의 책임을 정리해
클라이언트가 한 화면에서 필요한 값을 안정적으로 받을 수 있게 하는 것이 목적이다.

## 2. 범위

### In scope

- 온보딩 알림 설정 API에서 식사 시간/운동 시간 입력 제거.
- 마이페이지 알림 설정의 식사/운동 알림 수신 여부를 `recordReminderEnabled` 하나로 통합.
- 기록 입력 API의 운동 시간을 `exerciseDurationHours`, `exerciseDurationMinutes`로 분리 입력.
- 날짜별 기록 조회 응답에 작성자 연속 기록 일수 추가.
- 그룹원 조회 응답에 현재 로그인 회원 기준 미확인 기록 수 추가.
- 알림 목록 조회를 커서 페이징으로 변경.
- Swagger 성공/주요 예외 응답 테스트 보강.
- 관련 LLD/ADR 갱신.

### Out of scope

- 실제 푸시 발송 스케줄러.
- 댓글/기록 수정 및 삭제 API.
- 마이페이지 중복 그룹 API의 즉시 삭제.
- 미확인 기록 수의 푸시/배지 연동.

## 3. API 변경안

### 온보딩 알림 설정

```http
PUT /api/v1/onboarding/notifications
```

요청:

```json
{
  "recordReminderEnabled": true,
  "commentNotificationEnabled": true,
  "challengeNotificationEnabled": true
}
```

식사 시간은 `POST /api/v1/onboarding/profile` 또는 `PUT /api/v1/mypage/meal-times`에서만 입력한다.
운동 일정은 `POST /api/v1/onboarding/profile` 또는 `PUT /api/v1/mypage/exercise-schedules`에서만 입력한다.

### 마이페이지 알림 설정

```http
GET /api/v1/mypage/notification-settings
PATCH /api/v1/mypage/notification-settings
```

요청:

```json
{
  "recordReminderEnabled": true,
  "commentNotificationEnabled": true,
  "challengeNotificationEnabled": true
}
```

응답은 `recordReminderEnabled`, `commentNotificationEnabled`, `challengeNotificationEnabled`,
`mealSchedules`, `exerciseSchedules`를 포함한다.

### 기록 입력

```http
POST /api/v1/records
```

운동 기록 요청:

```json
{
  "groupId": 1,
  "recordType": "EXERCISE",
  "imageKey": "records/1/2026/07/4111584723969.jpg",
  "mealTime": null,
  "menu": null,
  "exerciseDurationHours": 1,
  "exerciseDurationMinutes": 30,
  "exerciseName": "러닝"
}
```

서버 저장은 기존 `exercise_duration_minutes` 총 분 단위를 유지한다.

### 날짜별 기록 조회

```http
GET /api/v1/groups/{groupId}/records?date=2026-07-01&cursor=100&size=20
```

각 기록 응답에 `recordingStreakDays`를 추가한다.
기준 날짜부터 과거로 하루도 빠지지 않고 기록한 날짜 수다.

### 그룹원 조회

```http
GET /api/v1/groups/{groupId}/members
```

각 구성원 응답에 `unreadRecordCount`를 추가한다.
현재 로그인 회원이 해당 구성원의 기록 상세에 마지막으로 진입한 시각 이후 올라온 기록 수다.
본인 행은 `0`으로 응답한다.

### 기록 상세 캐러셀 조회

```http
GET /api/v1/records/{recordId}?cursor=100&size=20
```

응답은 `totalCount`, `currentIndex`, `records`, `nextCursor`, `hasNext`를 포함한다.
최초 조회 시(`cursor == null`), 타겟 `recordId`가 첫 페이지의 맨 처음 인덱스(`currentIndex = 0`)로 노출되도록 `record.id >= recordId` 조건으로 쿼리한다.
이후 `nextCursor`를 사용해 다음 페이지 목록을 연속으로 캐러셀 스크롤할 수 있다.

### 알림 목록 조회

```http
GET /api/v1/notifications?cursor=100&size=20
```

응답은 `notifications`, `nextCursor`, `hasNext`를 포함한다.
정렬은 최신순이며 `nextCursor`는 마지막 알림 id다.

## 4. 데이터 모델

- `notification_setting`
  - DB의 `meal_reminder_enabled`, `exercise_reminder_enabled`는 유지한다.
  - API에서는 `recordReminderEnabled` 하나로 노출하고 두 DB 필드에 같은 값을 저장한다.
- `record_view_history`
  - `viewer_member_id`, `group_id`, `writer_member_id`, `last_viewed_at`.
  - 외래키 제약조건 없이 id 값으로만 참조한다.
  - 그룹원별 미확인 기록 수 계산 기준으로 사용한다.
  - 상세 진입 기준이므로 기록별 읽음 상태는 저장하지 않는다.
- `activity_record`
  - 운동 시간은 기존 `exercise_duration_minutes` 총 분 저장 방식을 유지한다.

## 5. 처리 흐름

1. 알림 설정 API는 수신 여부만 수정하고 식사 시간/운동 일정은 변경하지 않는다.
2. 기록 입력 API는 운동 시간/분을 총 분으로 환산해 저장한다.
3. 날짜별 기록 조회는 페이지에 포함된 작성자별로 기준 날짜까지의 기록 날짜를 조회해 연속 일수를 계산한다.
4. 기록 상세 진입 시 그룹 기록이면 `record_view_history`를 생성하거나 `last_viewed_at`을 갱신한다.
5. 그룹원 조회는 `record_view_history.last_viewed_at` 이후의 활성 기록 수를 `unreadRecordCount`로 응답한다.
6. 알림 목록은 `size + 1`개를 조회해 `hasNext`와 `nextCursor`를 계산한다.
7. 기록 상세 캐러셀 조회는 `size + 1`개를 조회해 `hasNext`와 `nextCursor`를 계산한다. 최초 조회(`cursor == null`) 시에는 타겟 `recordId`가 첫 페이지의 맨 처음(`currentIndex = 0`)에 위치하도록 `record.id >= recordId`로 필터링하며, `totalCount`는 해당 날짜 전체 글 수를 별도 쿼리한다.

### 5.1 연속 기록 일수 계산

1차 구현은 조회 시점 계산을 사용한다.

```text
기준 날짜: 2026-07-04
작성자 기록 날짜: 2026-07-04, 2026-07-03, 2026-07-01
결과: 2일
```

처리 절차:

1. 날짜별 피드 페이지에 포함된 작성자 id를 중복 제거한다.
2. 작성자별로 기준 날짜 종료 시각 이전의 활성 그룹 기록을 날짜 역순으로 조회한다.
3. 같은 날짜에 여러 기록이 있으면 한 날짜로 취급한다.
4. 기준 날짜부터 하루씩 감소시키며 끊기기 전까지 카운트한다.

이 방식은 초기 구현에서는 단순하고 정확하지만 작성자 수만큼 추가 조회가 발생할 수 있다.
성능 문제가 확인되면 `member_record_streak` 같은 집계 테이블을 추가하고,
일 배치 또는 기록 생성/삭제 이벤트 기반 갱신으로 전환한다.

## 6. 검증 규칙

- 운동 기록은 `exerciseDurationHours`와 `exerciseDurationMinutes`의 합산 시간이 1분 이상 1440분 이하이어야 한다.
- 운동 분은 0~59 범위만 허용한다.
- 식사 기록은 운동 시간/분 필드를 비워야 한다.
- 알림 설정 수정은 시간표 필드를 받지 않는다.

## 7. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 회원 없음: `MEMBER302`.
- 그룹 없음: `GROUP302`.
- 그룹 참여 정보 없음: `GROUP306`.
- 기록 입력값 검증 실패: `RECORD301`.
- 마이페이지 입력값 검증 실패: `MYPAGE301`.
- 알림 없음: `NOTIFICATION302`.

## 8. 인수조건

- [x] 온보딩 알림 설정 API에서 식사/운동 시간 입력이 제거된다.
- [x] 마이페이지 알림 설정 API가 `recordReminderEnabled` 하나로 동작한다.
- [x] 운동 기록 입력이 시간/분 분리 요청으로 문서화된다.
- [x] 날짜별 기록 조회에 `recordingStreakDays`가 포함된다.
- [x] 그룹원 조회에 `unreadRecordCount`가 포함된다.
- [x] 알림 목록 조회가 커서 페이징으로 동작한다.
- [x] Swagger 성공/주요 예외 응답이 테스트로 생성된다.
- [x] `./gradlew build`가 통과한다.

## 9. 검토 필요

- 마이페이지 중복 그룹 API 제거 시점은 후속 작업에서 결정한다.
- streak 조회 비용은 데이터 증가 후 재평가한다.
