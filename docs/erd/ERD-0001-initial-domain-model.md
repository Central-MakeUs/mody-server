# ERD-0001: 초기 도메인 ERD 초안

| 항목 | 값 |
| --- | --- |
| 상태 | Draft |
| 날짜 | 2026-06-24 |
| 관련 | Issue #1, LLD-0004 |

## 핵심 정책

- 회원은 여러 그룹에 동시에 속할 수 있으며, 활성 그룹 수 제한은 두지 않는다.
- 그룹에 따라 노출되는 사용자 정보가 다르므로 그룹별 표시 닉네임/프로필 이미지는 `group_member`에 둔다.
- 그룹에서 탈퇴한 회원의 기존 기록, 댓글, 챌린지 기여도는 해당 그룹 화면에 노출하지 않는다.
- 현재 체중은 `member`에 저장하지 않고 최신 `weight_record`로 계산한다.
- 식사/운동 기록은 `activity_record` 하나로 관리한다.
- 식사/운동 기록에는 코멘트를 남길 수 있으며, 코멘트는 작성자 `member_id`와 내용만 저장한다.
- 운동 종류는 enum이 아니라 문자열로 저장한다.
- 챌린지는 걸음수형(`STEP`)과 사진 인증형(`PHOTO`)으로 나눈다.
- 걸음수 챌린지는 서비스 제공 마스터 데이터로 관리한다.
- 주간 챌린지는 기능정의서 기준 서비스 제공 목록 중 1~2개를 선택하는 방식으로 본다.
- 주간 챌린지는 그룹의 모든 활성 멤버가 1회씩 사진 인증하면 완료된다.
- 챌린지 뱃지는 그룹 단위로 발급한다.
- 챌린지 마감 문구는 `group_challenge.ends_on`의 요일로 계산한다.
- 챌린지 변경 시 기존 `group_challenge`를 `RESET` 상태로 종료하고 새 진행 인스턴스를 만든다.
- 이미지는 저장소 key를 DB에 저장하고, 접근 URL은 응답 시 생성한다.
- 알림 설정은 식사/운동, 코멘트, 챌린지 알림을 포함한다. 알림 발송 이력은 `notification`의 상태로 관리한다.
- 모든 주요 테이블은 `id`, `created_at`, `updated_at`, `deleted_at`, `status` 공통 컬럼을 가진다.
- DB 외래키 제약조건은 사용하지 않는다. 다이어그램의 `REF` 컬럼은 논리 참조 id다.

## Mermaid ERD

```mermaid
erDiagram
    MEMBER ||--o{ SOCIAL_ACCOUNT : has
    MEMBER ||--o{ WEIGHT_RECORD : records
    MEMBER ||--o{ GROUP_MEMBER : joins
    MEMBER ||--o{ ACTIVITY_RECORD : uploads
    MEMBER ||--o{ RECORD_COMMENT : writes
    MEMBER ||--o| NOTIFICATION_SETTING : configures
    MEMBER ||--o{ EXERCISE_SCHEDULE : schedules
    MEMBER ||--o{ STEP_RECORD : contributes
    MEMBER ||--o{ CHALLENGE_PROOF : submits
    MEMBER ||--o{ NOTIFICATION : receives

    MODY_GROUP ||--o{ GROUP_MEMBER : contains
    MODY_GROUP ||--o{ ACTIVITY_RECORD : shows
    MODY_GROUP ||--o{ GROUP_CHALLENGE : runs
    MODY_GROUP ||--o{ CHALLENGE_BADGE : earns

    ACTIVITY_RECORD ||--o{ RECORD_COMMENT : has

    CHALLENGE ||--o{ GROUP_CHALLENGE : instantiated_as
    CHALLENGE ||--o| STEP_CHALLENGE_DETAIL : has_step_goal
    CHALLENGE ||--o{ CHALLENGE_BADGE : awards
    GROUP_CHALLENGE ||--o{ STEP_RECORD : aggregates
    GROUP_CHALLENGE ||--o{ CHALLENGE_PROOF : collects
    GROUP_CHALLENGE ||--o| CHALLENGE_BADGE : issues

    MEMBER {
        bigint id PK
        varchar nickname
        date birth_date
        decimal target_weight_kg
        varchar profile_image_key
        varchar health_connection_status
    }

    SOCIAL_ACCOUNT {
        bigint id PK
        bigint member_id REF
        varchar login_type
        varchar provider_user_id
    }

    WEIGHT_RECORD {
        bigint id PK
        bigint member_id REF
        date recorded_on
        decimal weight_kg
        decimal change_from_previous_kg
    }

    MODY_GROUP {
        bigint id PK
        varchar code
        varchar name
    }

    GROUP_MEMBER {
        bigint id PK
        bigint member_id REF
        bigint group_id REF
        varchar group_member_status
        varchar display_nickname
        varchar display_profile_image_key
        datetime joined_at
        datetime left_at
    }

    ACTIVITY_RECORD {
        bigint id PK
        bigint member_id REF
        bigint group_id REF
        varchar record_type
        time meal_time
        varchar menu
        int exercise_duration_minutes
        varchar exercise_name
        varchar image_key
        datetime uploaded_at
    }

    RECORD_COMMENT {
        bigint id PK
        bigint record_id REF
        bigint member_id REF
        varchar content
    }

    NOTIFICATION_SETTING {
        bigint id PK
        bigint member_id REF
        boolean meal_reminder_enabled
        time breakfast_time
        time lunch_time
        time dinner_time
        boolean exercise_reminder_enabled
        time exercise_time
        boolean streak_notification_enabled
        boolean comment_notification_enabled
        boolean challenge_notification_enabled
    }

    NOTIFICATION {
        bigint id PK
        bigint receiver_member_id REF
        varchar notification_type
        varchar notification_status
        varchar title
        varchar content
        datetime sent_at
        datetime read_at
    }

    EXERCISE_SCHEDULE {
        bigint id PK
        bigint member_id REF
        varchar day_of_week
        time day_time
        time evening_time
    }

    CHALLENGE {
        bigint id PK
        varchar challenge_type
        varchar title
        varchar description
    }

    GROUP_CHALLENGE {
        bigint id PK
        bigint group_id REF
        bigint challenge_id REF
        date starts_on
        date ends_on
        varchar group_challenge_status
        datetime completed_at
        datetime ended_at
    }

    STEP_CHALLENGE_DETAIL {
        bigint id PK
        bigint challenge_id REF
        varchar departure
        varchar destination
        decimal distance_km
        int target_step_count
        int display_order
    }

    STEP_RECORD {
        bigint id PK
        bigint group_challenge_id REF
        bigint member_id REF
        date recorded_on
        int step_count
        varchar step_source
    }

    CHALLENGE_PROOF {
        bigint id PK
        bigint group_challenge_id REF
        bigint member_id REF
        varchar image_key
        datetime uploaded_at
    }

    CHALLENGE_BADGE {
        bigint id PK
        bigint group_id REF
        bigint challenge_id REF
        bigint group_challenge_id REF
        datetime issued_at
    }
```

## 다이어그램 반영 메모

- `member`와 `mody_group`은 `group_member`로 N:M 관계를 가진다.
- 그룹 최대 인원과 회원별 활성 그룹 수 제한은 두지 않는다.
- "그룹과 함께한지 N일"은 해당 회원의 `group_member.joined_at` 기준으로 계산한다.
- `activity_record.group_id`는 그룹 미참여 시 본인 기록만 노출할 수 있도록 nullable로 둔다.
- 그룹 탈퇴 후에는 `group_member.status != JOINED`로 간주하여 그룹 피드/챌린지 노출에서 제외한다.
- 코멘트 목록은 `record_comment.member_id`로 회원 또는 그룹별 표시 정보를 조회해 닉네임과 내용을 응답한다.
- `challenge`는 마스터 데이터이고, 실제 그룹별 진행 상태는 `group_challenge`에 저장한다.
- 걸음수 챌린지는 `step_challenge_detail.target_step_count`와 `step_record.step_count`로 집계한다.
- 걸음수 기여도 순위는 특정 `group_challenge_id`의 `step_record`를 회원별 합산해서 계산한다.
- 지금까지 걸어간 지역 내역은 `COMPLETED` 또는 `RESET` 처리된 `group_challenge`와 연결된 `step_challenge_detail`로 조회한다.
- 챌린지 변경 시 기존 `step_record`는 물리 삭제하지 않고 이전 `group_challenge`에 묶어 보존한다. 새 챌린지는 새 `group_challenge`로 시작하므로 현재 진행률은 0부터 계산된다.
- 사진 인증형 챌린지는 `challenge_proof`에 멤버별 인증 이미지를 저장한다.
- 주간 챌린지는 모든 활성 그룹 멤버의 `challenge_proof`가 1개 이상 있으면 완료된다.
- 챌린지 완료 시 `challenge_badge`를 그룹 단위로 발급한다.
- 알림은 `notification_setting`으로 수신 여부를 판단하고, 실제 발송 이력은 `notification.notification_status`로 관리한다.

## 걸음수 챌린지 마스터

| 출발 | 도착 | 거리(km) | 목표 걸음수 |
| --- | --- | ---: | ---: |
| 서울 | 인천 | 60 | 150,000 |
| 서울 | 천안 | 90 | 200,000 |
| 서울 | 대전 | 160 | 300,000 |
| 서울 | 대구 | 240 | 400,000 |
| 서울 | 부산 | 325 | 500,000 |
| 서울 | 제주 | 500 | 700,000 |

## 확인 필요

- 주간 챌린지가 매주 자동 배정인지, 그룹이 서비스 제공 목록에서 선택하는지 최종 확인.
- soft delete 후 unique 재사용 대상별 suffix 처리 규칙.
