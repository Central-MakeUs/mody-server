# LLD-0015: 온보딩 개별 API

> Low-Level Design. 이 문서는 온보딩 개별 API 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #44 |
| 관련 ADR | ADR-0003 |
| 작성자 | Codex |
| 작성일 | 2026-07-02 |

## 1. 목적 / 배경

온보딩 화면에서 입력되는 체중, 알림 설정, 건강 앱 연동 상태, 그룹 참여/생성 정보를
샘플 응답이 아니라 실제 회원 데이터로 저장한다.

## 2. 범위

### In scope

- 통합 개인 정보 입력 API 요청 규칙 명세 보강.
- 체중 입력 API 실제 저장.
- 기록/댓글/챌린지 알림 수신 여부 실제 저장.
- 건강 앱 연동 상태 저장.
- 온보딩 그룹 참여/생성 API를 기존 `GroupService`로 위임.
- Swagger 성공/주요 예외 응답 명세 보강.

### Out of scope

- 마이페이지 알림 설정 조회/수정.
- 그룹 생성/참여 정책 변경.
- 건강 앱 provider별 세부 권한 저장.

## 3. 인터페이스 / API

모든 구현 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
POST /api/v1/onboarding/profile
POST /api/v1/onboarding/weight
PUT /api/v1/onboarding/notifications
PUT /api/v1/onboarding/health-connection
POST /api/v1/onboarding/groups/join
POST /api/v1/onboarding/groups
```

온보딩 그룹 API는 동일한 비즈니스 로직을 재사용하기 위해 `GroupService`에 위임한다.

### 개인 정보 입력 요청 규칙

`POST /api/v1/onboarding/profile`은 다음 규칙을 만족해야 한다.

- `nickname`: 필수, 14자 이하, 그룹 내 중복 허용.
- `birthDate`: 필수, `yyyy-MM-dd`, 과거 날짜.
- `currentWeightKg`, `targetWeightKg`: 필수, 20.0~300.0kg, 소수점 둘째 자리까지 허용.
- `mealSchedules`: 필수, 정확히 3개. `BREAKFAST`, `LUNCH`, `DINNER`를 각각 1개씩 입력.
- `mealSchedules[].skipped=false`: `time` 필수, `HH:mm`.
- `mealSchedules[].skipped=true`: `time`은 `null`.
- `exerciseSchedules`: 필수, 최소 3개.
- `exerciseSchedules[].dayOfWeek`: `MONDAY`~`SUNDAY`.
- `exerciseSchedules[].time`: 필수, `HH:mm`.

예를 들어 식사에서 `skipped=true`, `time=null`은 정상 요청이다.
운동 일정이 2개뿐이면 주 3회 이상 정책을 만족하지 않아 `MEMBER301`로 실패한다.

## 4. 데이터 모델

- `member`
  - `target_weight_kg`: 목표 체중 저장.
  - `health_connection_status`: 건강 앱 연동 여부 저장.
  - `group_onboarding_completed`: 그룹 생성/참여 성공 시 `true` 유지.
- `weight_record`
  - 현재 체중과 이전 기록 대비 증감 값을 저장한다.
- `notification_setting`
  - 기록 알림 수신 여부는 API에서 `recordReminderEnabled` 하나로 받고,
    내부 `meal_reminder_enabled`, `exercise_reminder_enabled`에는 같은 값을 저장한다.
  - 댓글/챌린지 알림 수신 여부를 저장한다.
  - 식사 시간은 통합 프로필 입력 또는 마이페이지 식사 시간 수정 API에서 저장한다.
- `exercise_schedule`
  - 통합 프로필 입력 시 요일별 운동 일정을 저장한다.

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

1. `@CurrentMember`가 access token에서 회원 id를 추출한다.
2. 활성 회원을 조회한다.
3. 체중 입력은 `member.target_weight_kg`를 갱신하고 `weight_record`를 추가한다.
4. 알림 설정은 수신 여부만 변경하고 식사 시간/운동 일정은 변경하지 않는다.
5. 건강 앱 연동은 `member.health_connection_status`를 갱신한다.
6. 그룹 참여/생성은 `GroupService`에 위임한다.

## 6. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 회원 없음: `MEMBER302`.
- 회원 가입 입력값 검증 실패: `MEMBER301`.
  - 식사 설정이 3개가 아니거나, 아침/점심/저녁 중복/누락이 있는 경우.
  - `skipped=false`인데 `time`이 없거나, `skipped=true`인데 `time`이 있는 경우.
  - 운동 일정이 3개 미만이거나 요일/시간이 누락된 경우.
- 이미 개인 정보 입력 완료: `MEMBER303`.
- 그룹 관련 오류: `GROUP302`~`GROUP307`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 온보딩 체중 입력이 DB에 저장된다.
- [x] 온보딩 알림 설정이 DB에 저장된다.
- [x] 건강 앱 연동 상태가 회원에 저장된다.
- [x] 온보딩 그룹 API는 기존 그룹 생성/참여 정책을 재사용한다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.
- [x] 관련 테스트와 `./gradlew build`가 통과한다.

## 8. 영향 범위 / 마이그레이션

- `OnboardingService`에 개별 저장 유스케이스가 추가된다.
- `Member`와 `NotificationSetting`에 상태 갱신 메서드가 추가된다.
- `NotificationSettingRepository`에 회원별 조회 메서드가 추가된다.

## 9. 미결정 사항 (Open Questions)

- 온보딩 통합 프로필 API와 개별 API를 장기적으로 모두 유지할지, 개별 API 중심으로 수렴할지 결정이 필요하다.

## 10. 참고

- `docs/lld/LLD-0008-group-api.md`
