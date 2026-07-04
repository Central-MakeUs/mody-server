# LLD-0016: 마이페이지 설정 API

> Low-Level Design. 이 문서는 마이페이지 설정 API 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #45 |
| 관련 ADR | ADR-0003, ADR-0011 |
| 작성자 | Codex |
| 작성일 | 2026-07-04 |

## 1. 목적 / 배경

마이페이지에서 회원이 알림 on/off, 식사 시간, 운동 일정을 수정할 수 있어야 한다.
온보딩에서 입력한 식사/운동 설정과 같은 저장 구조를 사용하므로,
설정 저장 로직은 온보딩과 마이페이지가 공통 컴포넌트를 통해 사용한다.

## 2. 범위

### In scope

- 알림 설정 조회.
- 알림 on/off 수정.
- 식사 시간 및 먹지 않음 수정.
- 운동 요일/시간 일정 수정.
- 온보딩 설정 저장 로직 공통화.
- Swagger 성공/주요 예외 응답 명세.

### Out of scope

- 실제 푸시 발송.
- 알림 발송 이력 생성.
- 식사/운동 알림 스케줄러.
- 회원 탈퇴 및 그룹 나가기.

## 3. 인터페이스 / API

모든 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
GET /api/v1/mypage/notification-settings
PATCH /api/v1/mypage/notification-settings
PUT /api/v1/mypage/meal-times
PUT /api/v1/mypage/exercise-schedules
```

알림 설정 수정 요청:

```json
{
  "recordReminderEnabled": true,
  "commentNotificationEnabled": true,
  "challengeNotificationEnabled": true
}
```

식사 시간 수정 요청:

```json
{
  "mealSchedules": [
    {"mealType": "BREAKFAST", "time": "08:00", "skipped": false},
    {"mealType": "LUNCH", "time": null, "skipped": true},
    {"mealType": "DINNER", "time": "18:00", "skipped": false}
  ]
}
```

운동 일정 수정 요청:

```json
{
  "schedules": [
    {"dayOfWeek": "MONDAY", "time": "07:30"},
    {"dayOfWeek": "WEDNESDAY", "time": "20:00"},
    {"dayOfWeek": "FRIDAY", "time": "09:00"}
  ]
}
```

## 4. 데이터 모델

- `notification_setting`
  - `meal_reminder_enabled`, `exercise_reminder_enabled`
    - API에서는 `recordReminderEnabled` 하나로 노출하고 두 DB 필드에 같은 값을 저장한다.
  - `comment_notification_enabled`, `challenge_notification_enabled`
  - `breakfast_time`, `lunch_time`, `dinner_time`
- `exercise_schedule`
  - `member_id`, `day_of_week`, `scheduled_time`

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

1. `@CurrentMember`가 access token에서 회원 id를 추출한다.
2. 마이페이지 서비스는 활성 회원인지 검증한다.
3. `NotificationPreferenceService`가 알림 설정과 운동 일정을 조회/저장한다.
4. 알림 on/off 수정은 시간표를 변경하지 않고 기록 알림 수신 여부를 하나의 값으로 저장한다.
5. 식사 시간 수정은 아침/점심/저녁 3개를 모두 받아 `notification_setting`의 식사 시간만 갱신한다.
6. 운동 일정 수정은 기존 활성 운동 일정을 soft delete 후 새 일정 목록을 저장한다.

## 6. 검증 규칙

- 식사 설정은 `BREAKFAST`, `LUNCH`, `DINNER`를 각각 1개씩 입력한다.
- `skipped == true`이면 식사 시간은 `null`이어야 한다.
- `skipped == false`이면 식사 시간은 필수다.
- 운동 일정은 최소 3개 이상 입력한다.
- 운동 일정은 `dayOfWeek`와 `time`을 모두 입력한다.

## 7. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 요청 검증 실패: `MYPAGE301`.
- 회원 없음: `MEMBER302`.

## 8. 인수조건 (Acceptance Criteria)

- [x] 인증 회원이 알림 설정을 조회할 수 있다.
- [x] 인증 회원이 알림 on/off를 수정할 수 있다.
- [x] 인증 회원이 식사 시간과 먹지 않음 여부를 수정할 수 있다.
- [x] 인증 회원이 운동 요일/시간 일정을 주 3회 이상으로 수정할 수 있다.
- [x] 온보딩과 마이페이지가 같은 설정 저장 컴포넌트를 사용한다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.
- [x] 관련 테스트가 통과한다.

## 9. 영향 범위 / 마이그레이션

- 기존 마이페이지 설정 stub API가 DB 기반 구현으로 교체된다.
- `NotificationPreferenceService`가 추가되어 온보딩 설정 저장 로직도 이 컴포넌트를 사용한다.
- 운동 일정 수정은 기존 활성 row를 soft delete하고 새 row를 추가한다.

## 10. 미결정 사항 (Open Questions)

- 식사/운동 알림 on/off가 꺼진 상태에서 개별 시간 값을 유지할지 삭제할지는 추후 정책 확정이 필요하다.
- 운동 일정의 같은 요일 중복 허용 여부는 추후 정책 확정이 필요하다.
