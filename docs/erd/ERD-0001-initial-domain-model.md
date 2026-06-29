# ERD-0001: 초기 도메인 ERD

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-06-24 |
| 코드 동기화 | 2026-06-29 |
| 관련 | Issue #1, LLD-0004, ADR-0003 |

## 목적

현재 코드의 JPA 엔티티를 기준으로 초기 도메인 데이터 모델을 기록한다.
이 문서는 실제 테이블 컬럼, 논리 참조, enum, 주요 인덱스를 맞춰 관리하는 기준 문서다.

## 공통 모델링 규칙

- 모든 엔티티는 `BaseEntity`를 상속한다.
- 공통 컬럼은 `id`, `created_at`, `updated_at`, `deleted_at`, `status`다.
- `status` 값은 `ACTIVE`, `INACTIVE`다.
- PK는 애플리케이션에서 생성한 `Long` id를 사용한다.
- DB 외래키 제약조건과 JPA 연관관계는 사용하지 않는다.
- 다이어그램의 `REF` 컬럼은 DB FK가 아니라 논리 참조 id다.
- enum은 문자열로 저장한다.
- soft delete는 `status = INACTIVE`, `deleted_at` 기록으로 표현한다.

## 현재 코드 기준 정책

- 회원의 현재 체중은 `member`에 저장하지 않고 최신 `weight_record`로 계산한다.
- 개인 정보 입력 완료 여부는 `member.birth_date`와 `member.target_weight_kg` 존재 여부로 판단한다.
- 회원은 동시에 최대 4개 그룹에 참여할 수 있다.
- 그룹은 최대 12명의 활성 멤버를 가질 수 있다.
- 그룹 코드는 6자리이며, `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` 문자 집합으로 생성한다.
- 그룹별 표시 닉네임과 프로필 이미지는 가입 시점의 `group_member`에 저장한다.
- 그룹 탈퇴 시 `group_member_status = LEFT`, `left_at`, 공통 soft delete 값을 함께 기록한다.
- 식사/운동 기록은 `activity_record` 하나로 관리한다.
- `activity_record.group_id`는 nullable이며, 그룹에 묶이지 않은 개인 기록을 허용한다.
- 식사/운동 기록 이미지는 서버가 보관하는 파일이 아니라 스토리지 `image_key`로 저장한다.
- 운동 일정은 요일과 단일 시간(`scheduled_time`) 조합으로 저장한다.
- 알림 발송 이력은 `notification.notification_status`로 관리한다.
- refresh token은 Redis가 아니라 `refresh_token` 테이블에 저장한다.

## Mermaid ERD

```mermaid
erDiagram
    MEMBER ||--o{ SOCIAL_ACCOUNT : owns
    MEMBER ||--o{ REFRESH_TOKEN : issues
    MEMBER ||--o{ WEIGHT_RECORD : records
    MEMBER ||--o{ GROUP_MEMBER : joins
    MEMBER ||--o{ ACTIVITY_RECORD : uploads
    MEMBER ||--o{ RECORD_COMMENT : writes
    MEMBER ||--o| NOTIFICATION_SETTING : configures
    MEMBER ||--o{ EXERCISE_SCHEDULE : schedules
    MEMBER ||--o{ NOTIFICATION : receives
    MEMBER ||--o{ STEP_RECORD : contributes
    MEMBER ||--o{ CHALLENGE_PROOF : submits

    MODY_GROUP ||--o{ GROUP_MEMBER : contains
    MODY_GROUP ||--o{ ACTIVITY_RECORD : scopes
    MODY_GROUP ||--o{ GROUP_CHALLENGE : runs
    MODY_GROUP ||--o{ CHALLENGE_BADGE : earns

    ACTIVITY_RECORD ||--o{ RECORD_COMMENT : has

    CHALLENGE ||--o{ GROUP_CHALLENGE : instantiates
    CHALLENGE ||--o| STEP_CHALLENGE_DETAIL : has_step_goal
    GROUP_CHALLENGE ||--o{ STEP_RECORD : aggregates
    GROUP_CHALLENGE ||--o{ CHALLENGE_PROOF : collects
    GROUP_CHALLENGE ||--o| CHALLENGE_BADGE : issues

    MEMBER {
        bigint id PK
        varchar nickname "not null, len 14"
        date birth_date "nullable"
        decimal target_weight_kg "nullable, 5,2"
        varchar profile_image_key "nullable, len 500"
        varchar health_connection_status "not null, len 20"
    }

    SOCIAL_ACCOUNT {
        bigint id PK
        bigint member_id REF
        varchar login_type "not null, len 20"
        varchar provider_user_id "not null, len 100"
    }

    REFRESH_TOKEN {
        bigint id PK
        bigint member_id REF
        varchar token "not null, len 500"
    }

    WEIGHT_RECORD {
        bigint id PK
        bigint member_id REF
        date recorded_on "not null"
        decimal weight_kg "not null, 5,2"
        decimal change_from_previous_kg "not null, 5,2"
    }

    MODY_GROUP {
        bigint id PK
        varchar code "not null, len 6"
        varchar name "not null, len 30"
    }

    GROUP_MEMBER {
        bigint id PK
        bigint member_id REF
        bigint group_id REF
        varchar group_member_status "not null, len 20"
        varchar display_nickname "nullable, len 14"
        varchar display_profile_image_key "nullable, len 500"
        datetime joined_at "not null"
        datetime left_at "nullable"
    }

    ACTIVITY_RECORD {
        bigint id PK
        bigint member_id REF
        bigint group_id REF "nullable"
        varchar record_type "not null, len 20"
        time meal_time "nullable"
        varchar menu "nullable, len 100"
        int exercise_duration_minutes "nullable"
        varchar exercise_name "nullable, len 30"
        varchar image_key "not null, len 500"
        datetime uploaded_at "not null"
    }

    RECORD_COMMENT {
        bigint id PK
        bigint record_id REF
        bigint member_id REF
        varchar content "not null, len 100"
    }

    NOTIFICATION_SETTING {
        bigint id PK
        bigint member_id REF
        boolean meal_reminder_enabled "not null"
        time breakfast_time "nullable"
        time lunch_time "nullable"
        time dinner_time "nullable"
        boolean exercise_reminder_enabled "not null"
        time exercise_time "nullable"
        boolean streak_notification_enabled "not null"
        boolean comment_notification_enabled "not null"
        boolean challenge_notification_enabled "not null"
    }

    EXERCISE_SCHEDULE {
        bigint id PK
        bigint member_id REF
        varchar day_of_week "not null, len 20"
        time scheduled_time "nullable"
    }

    NOTIFICATION {
        bigint id PK
        bigint receiver_member_id REF
        varchar notification_type "not null, len 30"
        varchar notification_status "not null, len 20"
        varchar title "not null, len 100"
        varchar content "not null, len 500"
        datetime sent_at "nullable"
        datetime read_at "nullable"
    }

    CHALLENGE {
        bigint id PK
        varchar challenge_type "not null, len 20"
        varchar title "not null, len 50"
        varchar description "nullable, len 500"
    }

    GROUP_CHALLENGE {
        bigint id PK
        bigint group_id REF
        bigint challenge_id REF
        date starts_on "not null"
        date ends_on "not null"
        varchar group_challenge_status "not null, len 20"
        datetime completed_at "nullable"
        datetime ended_at "nullable"
    }

    STEP_CHALLENGE_DETAIL {
        bigint id PK
        bigint challenge_id REF
        varchar departure "not null, len 30"
        varchar destination "not null, len 30"
        decimal distance_km "not null, 6,1"
        int target_step_count "not null"
        int display_order "not null"
    }

    STEP_RECORD {
        bigint id PK
        bigint group_challenge_id REF
        bigint member_id REF
        date recorded_on "not null"
        int step_count "not null"
        varchar step_source "not null, len 20"
    }

    CHALLENGE_PROOF {
        bigint id PK
        bigint group_challenge_id REF
        bigint member_id REF
        varchar image_key "not null, len 500"
        datetime uploaded_at "not null"
    }

    CHALLENGE_BADGE {
        bigint id PK
        bigint group_id REF
        bigint challenge_id REF
        bigint group_challenge_id REF
        datetime issued_at "not null"
    }
```

## Enum 값

| enum | 값 |
| --- | --- |
| `LoginType` | `KAKAO`, `APPLE`, `GOOGLE` |
| `HealthConnectionStatus` | `DISCONNECTED`, `CONNECTED` |
| `GroupMemberStatus` | `JOINED`, `LEFT` |
| `RecordType` | `MEAL`, `EXERCISE` |
| `NotificationType` | `GROUP_JOINED`, `RECORD_REMINDER`, `COMMENT`, `STREAK`, `NUDGE`, `CHALLENGE` |
| `NotificationStatus` | `PENDING`, `SENT`, `READ`, `FAILED` |
| `ChallengeType` | `STEP`, `PHOTO` |
| `GroupChallengeStatus` | `IN_PROGRESS`, `COMPLETED`, `RESET`, `CANCELED` |
| `StepSource` | `HEALTH_KIT`, `HEALTH_CONNECT`, `API`, `MANUAL` |
| `Status` | `ACTIVE`, `INACTIVE` |

## 주요 인덱스

| 테이블 | 인덱스 | 컬럼 |
| --- | --- | --- |
| `social_account` | `idx_social_account_provider_user` | `login_type`, `provider_user_id` |
| `weight_record` | `idx_weight_record_member_date` | `member_id`, `recorded_on` |
| `mody_group` | `idx_mody_group_code` | `code` |
| `group_member` | `idx_group_member_member` | `member_id` |
| `group_member` | `idx_group_member_group` | `group_id` |
| `activity_record` | `idx_activity_record_group_uploaded` | `group_id`, `uploaded_at` |
| `activity_record` | `idx_activity_record_member_uploaded` | `member_id`, `uploaded_at` |
| `record_comment` | `idx_record_comment_record` | `record_id` |
| `exercise_schedule` | `idx_exercise_schedule_member_day` | `member_id`, `day_of_week` |
| `notification` | `idx_notification_receiver_status` | `receiver_member_id`, `notification_status` |
| `notification` | `idx_notification_receiver_created` | `receiver_member_id`, `created_at` |
| `group_challenge` | `idx_group_challenge_group_period` | `group_id`, `starts_on`, `ends_on` |
| `step_record` | `idx_step_record_challenge_date` | `group_challenge_id`, `recorded_on` |
| `step_record` | `idx_step_record_member_date` | `member_id`, `recorded_on` |
| `challenge_proof` | `idx_challenge_proof_challenge` | `group_challenge_id` |
| `challenge_proof` | `idx_challenge_proof_member` | `member_id` |
| `challenge_badge` | `idx_challenge_badge_group` | `group_id` |
| `challenge_badge` | `idx_challenge_badge_group_challenge` | `group_challenge_id` |

## 도메인별 조회 기준

### 회원/인증

- 소셜 로그인은 `social_account.login_type + provider_user_id`로 회원을 찾는다.
- API 인증용 refresh token은 `refresh_token.member_id`에 묶어 저장한다.
- 회원 탈퇴나 로그아웃 정책에 따라 refresh token row를 삭제하거나 soft delete 처리한다.

### 그룹

- `member`와 `mody_group`은 `group_member`로 N:M 관계를 가진다.
- 활성 그룹 참여 여부는 `group_member_status = JOINED`와 `deleted_at is null`을 함께 본다.
- "그룹과 함께한지 N일"은 해당 회원의 `group_member.joined_at` 기준으로 계산한다.
- 그룹 구성원 조회 응답의 닉네임과 프로필 이미지는 `group_member`의 표시 정보를 우선 사용한다.

### 기록/댓글

- 기록 피드는 `activity_record.group_id`, `uploaded_at` 기준으로 조회한다.
- 개인 기록 목록은 `activity_record.member_id`, `uploaded_at` 기준으로 조회한다.
- 댓글은 `record_comment.record_id`로 조회하고, 작성자 표시는 `member_id`로 회원 또는 그룹 표시 정보를 조합한다.

### 알림

- 알림 수신 가능 여부는 `notification_setting`으로 판단한다.
- 알림 목록은 `notification.receiver_member_id`, `created_at` 기준으로 조회한다.
- 읽음 여부는 `notification_status = READ` 또는 `read_at` 존재 여부로 판단한다.

### 챌린지

- `challenge`는 서비스 제공 마스터 데이터다.
- `group_challenge`는 그룹별 진행 인스턴스다.
- 챌린지 마감 요일은 `group_challenge.ends_on.getDayOfWeek()`로 계산한다.
- 걸음수 챌린지 진행률은 `step_record.step_count`를 `group_challenge_id` 기준으로 합산한다.
- 걸음수 기여도 순위는 `group_challenge_id + member_id` 기준 합산값으로 계산한다.
- 사진 인증형 챌린지는 `challenge_proof`에 멤버별 인증 이미지를 저장한다.
- 챌린지 완료 시 `challenge_badge`를 그룹 단위로 발급한다.

## 걸음수 챌린지 마스터 데이터

| 출발 | 도착 | 거리(km) | 목표 걸음수 |
| --- | --- | ---: | ---: |
| 서울 | 인천 | 60 | 150,000 |
| 서울 | 천안 | 90 | 200,000 |
| 서울 | 대전 | 160 | 300,000 |
| 서울 | 대구 | 240 | 400,000 |
| 서울 | 부산 | 325 | 500,000 |
| 서울 | 제주 | 500 | 700,000 |

## 코드 동기화 메모

- 이 문서는 `src/main/java/cmc/mody/**/domain`의 현재 엔티티 기준이다.
- 아직 구현되지 않은 유스케이스 정책은 ERD 컬럼으로 확정하지 않는다.
- 새로운 엔티티, 컬럼, enum, 인덱스가 추가되면 이 문서를 함께 수정한다.
