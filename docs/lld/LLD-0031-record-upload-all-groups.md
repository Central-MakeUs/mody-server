# LLD-0031: 기록 업로드 전체 그룹 노출

> Low-Level Design. 이 문서는 기록 업로드 전체 그룹 노출 정책 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #103 |
| 관련 ADR | ADR-0007, ADR-0015, ADR-0016 |
| 작성자 | Codex |
| 작성일 | 2026-07-13 |

## 1. 목적 / 배경

식사/운동 기록은 특정 그룹을 선택해 올리는 것이 아니라, 작성자가 현재 참여 중인 모든 그룹에 동시에 노출된다.
기존 `POST /api/v1/records`는 `groupId`를 받아 단일 그룹 기록을 생성하므로,
기록 원본과 그룹별 노출/댓글/조회 이력을 분리해야 한다.

## 2. 범위

### In scope

- 기록 생성 요청에서 `groupId` 제거.
- 기록 원본 1개를 작성자 참여 그룹 전체에 노출.
- 그룹별 피드, 상세 캐러셀, 댓글, 미확인 기록 수 정합성 유지.
- Swagger 및 테스트 시나리오 갱신.

### Out of scope

- 기록 수정/삭제 정책.
- 과거 단일 그룹 기록의 데이터 마이그레이션. 개발 초기 데이터는 없다고 보고 별도 이관을 수행하지 않는다.

## 3. 인터페이스 / API

```http
POST /api/v1/records
```

식사 기록 요청:

```json
{
  "recordType": "MEAL",
  "imageKey": "records/1/2026/07/4111584723968.jpg",
  "mealTime": "12:30",
  "menu": "샐러드",
  "exerciseDurationHours": null,
  "exerciseDurationMinutes": null,
  "exerciseName": null
}
```

운동 기록 요청:

```json
{
  "recordType": "EXERCISE",
  "imageKey": "records/1/2026/07/4111584723969.jpg",
  "mealTime": null,
  "menu": null,
  "exerciseDurationHours": 0,
  "exerciseDurationMinutes": 40,
  "exerciseName": "러닝"
}
```

상세/댓글은 기록이 여러 그룹에 노출될 수 있으므로 그룹 컨텍스트를 명시한다.

```http
GET /api/v1/groups/{groupId}/records/{recordId}
GET /api/v1/groups/{groupId}/records/{recordId}/comments?cursor=10&size=20
POST /api/v1/groups/{groupId}/records/{recordId}/comments
```

## 4. 데이터 모델

### activity_record

- 기록 원본을 1개만 저장한다.
- `group_id`는 신규 생성 로직에서 사용하지 않는다.
- `member_id`, `record_type`, 기록 필드, `image_key`, `uploaded_at`을 원본 속성으로 사용한다.

### activity_record_group

기록이 어느 그룹에 노출되는지 저장하는 매핑 테이블이다.

- `id`: Snowflake id.
- `record_id`: 원본 기록 id.
- `group_id`: 노출 대상 그룹 id.
- `member_id`: 작성자 id. 그룹별 조회 최적화와 FK 없는 참조 정책을 위해 중복 저장한다.
- `uploaded_at`: 피드 정렬/조회 최적화를 위해 기록 생성 시각을 중복 저장한다.
- `deleted_at`: 기록 원본 삭제 시 함께 논리 삭제한다.

### record_comment

- 댓글은 그룹 맥락에 종속된다.
- `group_id`를 추가해 같은 원본 기록이라도 그룹별 댓글을 분리한다.
- 댓글 작성자 표시는 해당 그룹의 `group_member.display_nickname`, `display_profile_image_key`를 사용한다.

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

1. `@CurrentMember`가 access token에서 작성자 id를 추출한다.
2. 활성 회원을 조회한다.
3. 작성자가 `JOINED` 상태로 참여 중인 그룹 목록을 조회한다.
4. 참여 중인 그룹이 없으면 기록을 생성하지 않는다.
5. `recordType`에 따라 식사/운동 필드 조합과 이미지 키를 검증한다.
6. `activity_record` 원본을 1개 저장한다.
7. 작성자가 참여 중인 모든 그룹에 대해 `activity_record_group` 매핑을 생성한다.
8. 생성 응답은 원본 `recordId`와 노출된 `groupIds`를 반환한다.

## 6. 조회/댓글 영향

- 그룹 피드 조회는 `activity_record_group.group_id = :groupId` 기준으로 조회하고 원본 기록을 조인한다.
- 상세 캐러셀은 같은 `groupId`, 작성자, 날짜 기준으로 `activity_record_group`을 조회한다.
- 댓글 목록/작성은 `groupId + recordId` 접근 권한을 검증한다.
- 미확인 기록 수는 `activity_record_group` 기준으로 계산한다.
- 연속 기록 일수는 그룹별 피드에서 보이는 기록 기준이므로 `activity_record_group` 기준으로 계산한다.

## 7. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 회원 없음: `MEMBER302`.
- 참여 중인 그룹 없음: `GROUP306` 또는 신규 세부 코드.
- 기록 입력값 검증 실패: `RECORD301`.
- 기록 없음 또는 해당 그룹에 노출되지 않은 기록: `RECORD302`.
- 기록 타입 오류: `RECORD304`.
- 기록 이미지 키 오류: `RECORD305`.
- 식사 기록 payload 조합 오류: `RECORD306`.
- 운동 기록 payload 조합 오류: `RECORD307`.
- 운동 시간 범위 오류: `RECORD308`.
- 댓글 입력값 검증 실패: `RECORD309`.

## 8. 인수조건 (Acceptance Criteria)

- [x] 기록 생성 요청에서 `groupId`를 받지 않는다.
- [x] 작성자가 참여 중인 모든 그룹에 기록이 노출된다.
- [x] 작성자가 참여 중인 그룹이 없으면 기록 생성이 실패한다.
- [x] 그룹 피드 조회는 해당 그룹에 노출된 기록만 반환한다.
- [x] 상세/댓글 API는 그룹 컨텍스트를 기준으로 접근 권한을 검증한다.
- [x] 같은 원본 기록의 댓글은 그룹별로 분리된다.
- [x] 미확인 기록 수와 연속 기록 일수는 그룹별 노출 매핑 기준으로 계산된다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.

## 9. 영향 범위 / 마이그레이션

- `activity_record_group` 신규 테이블이 필요하다.
- `record_comment.group_id` 컬럼 추가가 필요하다.
- 신규 생성 로직은 `activity_record.group_id`를 사용하지 않는다.
- 기존 조회 쿼리는 `activity_record_group` 중심으로 재작성한다.
- 배포 DB에는 `activity_record_group` 테이블과 `record_comment.group_id` 컬럼을 반영해야 한다.
- Flyway 마이그레이션은 `V3__add_activity_record_group_mapping.sql`로 고정한다.
- 기존 개발 DB에 기록 데이터는 없다고 보고 매핑 데이터 이관 마이그레이션은 작성하지 않는다.
- 그룹 탈퇴 시 작성자의 원본 기록은 유지하고 해당 그룹의 노출 매핑과 그룹별 댓글만 삭제한다.
- 회원 탈퇴 시에는 작성자의 원본 기록까지 삭제한다. 원본 기록이 삭제되면 모든 그룹 노출 매핑과 댓글도 함께 삭제된다.

## 10. 정책 결정 사항

- 기록은 참여 중인 그룹이 하나 이상 있어야 생성할 수 있다.
- 기존 개발 DB에는 이관 대상 기록이 없으므로 기록 매핑 데이터 마이그레이션을 생략한다.
- 그룹 탈퇴 시 원본 기록은 유지한다. 해당 그룹에서만 더 이상 보이지 않도록 노출 매핑을 삭제한다.
- 댓글은 원본 기록과 그룹에 종속된다. 같은 사진/기록이라도 그룹별 댓글은 분리된다.
- 회원 탈퇴 시에는 작성자의 원본 기록까지 삭제한다.

## 11. 미결정 사항 (Open Questions)

- 없음.

## 12. 참고

- `docs/lld/LLD-0012-activity-record-create-api.md`
- `docs/lld/LLD-0013-activity-record-feed-api.md`
- `docs/lld/LLD-0014-record-detail-comment-api.md`
- `docs/adr/ADR-0015-group-member-unread-record-count.md`
- `docs/adr/ADR-0016-record-detail-carousel-paging.md`
