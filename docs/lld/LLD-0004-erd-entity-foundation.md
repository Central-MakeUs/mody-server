# LLD-0004: ERD 및 엔티티 기반 설계

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-06-24 |
| 관련 | ADR-0001, ADR-0003, ERD-0001 |

## 목표

온보딩, 그룹, 피드, 체중 기록, 알림, 챌린지 개발을 시작할 수 있도록 1차 엔티티 경계를 정의한다. 구현은 단일 모듈 안에서 feature 패키지를 유지한다.

DB 외래키 제약조건과 JPA 연관관계는 사용하지 않는다. 엔티티는 `memberId`, `groupId`, `recordId` 같은 `Long` id 컬럼으로 논리 참조만 저장한다.

## 엔티티 경계

- `member`: 닉네임, 생년월일, 목표 체중, 프로필 이미지 key, 건강 앱 연동 상태를 가진다. 현재 체중은 최신 `weight_record`로 계산한다.
- `social_account`: 카카오, 애플, 구글 계정 식별자를 저장한다.
- `weight_record`: 회원별 일자 단위 체중과 전일 대비 증감 값을 저장한다.
- `mody_group`: 그룹 코드, 그룹명을 저장한다. "그룹과 함께한지 N일"은 해당 회원의 `group_member.joinedAt` 기준으로 계산한다.
- `group_member`: 한 회원이 여러 그룹에 속할 수 있도록 회원-그룹 관계와 가입/탈퇴 시각을 저장한다. 활성 그룹 수 제한은 두지 않으며, 그룹별 표시 닉네임/프로필 이미지 key를 가진다.
- `activity_record`: 식사와 운동 기록을 하나의 피드 테이블로 저장한다. 식사는 식사 시간/메뉴, 운동은 운동 시간/운동명을 사용한다.
- `record_comment`: 피드 기록의 짧은 코멘트를 저장한다. `recordId`, `memberId`, `content`를 기본으로 하고, 닉네임은 조회 시 붙인다.
- `notification_setting`, `exercise_schedule`: 식사/운동 알림, 코멘트 알림, 챌린지 알림과 주 3회 이상 운동 일정을 저장한다.
- `notification`: 알림 발송 이력을 저장하고 `notificationStatus`로 발송/읽음/실패 상태를 관리한다.

## 챌린지

챌린지는 `challenge` 템플릿과 `group_challenge` 진행 인스턴스로 나눈다. `group_challenge.starts_on`, `group_challenge.ends_on`으로 진행 기간과 마감 요일을 계산한다. 화면 문구의 "일요일까지 마무리해야해요" 같은 값은 `ends_on.getDayOfWeek()` 기반으로 만든다.

- 걸음수 챌린지: `challenge.type = STEP`, 출발지/도착지/거리/목표 걸음수는 `step_challenge_detail`, 일자별 걸음수는 `step_record`에 저장한다.
- 사진 인증 챌린지: `challenge.type = PHOTO`, 인증 이미지는 `challenge_proof`에 저장한다. 기능정의서 기준 주간 챌린지는 서비스 제공 목록 중 1~2개를 선택하는 방식으로 본다.
- 주간 챌린지 완료 조건: 모든 활성 그룹 멤버가 1회 이상 사진 인증을 완료해야 한다.
- 챌린지 뱃지: 챌린지 완료 시 `challenge_badge`를 그룹 단위로 발급한다.
- 챌린지 변경: 기존 `group_challenge`는 `RESET` 상태와 `ended_at`을 기록하고, 새 `group_challenge`를 생성한다. 현재 진행률은 새 인스턴스 기준으로 0부터 계산한다.
- 걸어간 지역 내역: 완료 또는 변경 종료된 `group_challenge`와 `step_challenge_detail`로 조회한다.
- 기여도 순위: 특정 `group_challenge`의 `step_record`를 회원별 합산하여 계산한다.

## 걸음수 챌린지 마스터

| 출발 | 도착 | 거리(km) | 목표 걸음수 |
| --- | --- | ---: | ---: |
| 서울 | 인천 | 60 | 150,000 |
| 서울 | 천안 | 90 | 200,000 |
| 서울 | 대전 | 160 | 300,000 |
| 서울 | 대구 | 240 | 400,000 |
| 서울 | 부산 | 325 | 500,000 |
| 서울 | 제주 | 500 | 700,000 |

## 정책

- 그룹 최대 인원과 회원별 활성 그룹 수 제한은 두지 않는다.
- 그룹 탈퇴 후 기존 기록/댓글/챌린지 기여도는 해당 그룹 화면에서 노출하지 않는다.
- 운동 종류는 enum이 아니라 문자열로 저장한다.
- 운동 일정 최소 주 3회는 생성/수정 유스케이스에서 검증한다.
- 이미지는 저장소 key만 저장하고 URL은 응답 시 생성한다.
- soft delete 후 unique 값 재사용은 삭제 시 unique 컬럼 값에 삭제 suffix를 붙이는 방식으로 시작한다.
- 참조 id 유효성은 서비스 레이어에서 검증한다.

## 미결정 사항(Open Questions)

- [ ] Snowflake ID 생성기 적용.
- [ ] 주간 챌린지 자동 배정/그룹 선택 여부 최종 확정.
- [ ] 참조 id 유효성 검증 공통 패턴 확정.
- [ ] unique 컬럼별 soft delete suffix 처리 규칙 확정.
