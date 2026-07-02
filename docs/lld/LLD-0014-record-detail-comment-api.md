# LLD-0014: 기록 상세 및 댓글 API

> Low-Level Design. 이 문서는 기록 상세 및 댓글 API 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #43 |
| 관련 ADR | ADR-0007 |
| 작성자 | Codex |
| 작성일 | 2026-07-02 |

## 1. 목적 / 배경

기록 상세 화면에서 그룹원이 식사/운동 기록의 상세 정보와 댓글을 확인하고,
접근 가능한 기록에 댓글을 작성할 수 있게 한다.

## 2. 범위

### In scope

- 기록 상세 조회.
- 기록 댓글 목록 조회.
- 기록 댓글 작성.
- 기록/그룹 접근 권한 검증.
- 현재 그룹에서 나간 회원의 기록과 댓글 제외.
- Swagger 성공/예외 응답 명세 보강.

### Out of scope

- 댓글 수정/삭제.
- 댓글 알림 발송.
- 댓글 목록 별도 페이징.
- 이미지 read signed URL 발급.

## 3. 인터페이스 / API

모든 구현 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
GET /api/v1/records/{recordId}
POST /api/v1/records/{recordId}/comments
```

댓글 작성 요청:

```json
{
  "content": "좋다"
}
```

## 4. 데이터 모델

- `activity_record`
  - `id`, `group_id`, `member_id`, `record_type`, 기록 필드, `image_key`를 사용한다.
  - `deleted_at is null`인 기록만 조회한다.
- `record_comment`
  - `record_id`, `member_id`, `content`를 저장한다.
  - `deleted_at is null`인 댓글만 조회한다.
- `group_member`
  - 요청자와 작성자가 현재 `JOINED` 상태인지 확인한다.
  - 댓글 작성자 표시 정보는 `display_nickname`, `display_profile_image_key`를 사용한다.

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

1. `@CurrentMember`가 access token에서 회원 id를 추출한다.
2. 활성 회원을 조회한다.
3. 활성 기록을 조회한다.
4. 그룹 기록이면 요청자가 해당 그룹의 `JOINED` 회원인지 확인한다.
5. 그룹 기록의 작성자가 현재 그룹원이 아니면 기록을 찾을 수 없는 것으로 처리한다.
6. 상세 응답에 기록 정보와 현재 그룹원 댓글만 포함한다.
7. 댓글 작성은 접근 검증 후 Snowflake id를 발급해 `record_comment`에 저장한다.

개인 기록(`group_id is null`)은 작성자 본인만 상세 조회와 댓글 작성이 가능하다.

## 6. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 회원 없음: `MEMBER302`.
- 기록 없음 또는 노출 불가 기록: `RECORD302`.
- 그룹 없음: `GROUP302`.
- 그룹 참여 정보 없음: `GROUP306`.
- 댓글 입력값 검증 실패: `RECORD301`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 접근 가능한 기록 상세를 조회할 수 있다.
- [x] 상세 응답에 현재 그룹원 댓글 목록이 포함된다.
- [x] 접근 가능한 기록에 댓글을 작성할 수 있다.
- [x] 삭제 처리된 기록/댓글과 현재 그룹원이 아닌 작성자의 댓글은 노출되지 않는다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.
- [x] 서비스 단위 테스트가 주요 분기를 검증한다.

## 8. 영향 범위 / 마이그레이션

- `ActivityRecordService`에 상세 조회와 댓글 작성 기능이 추가된다.
- `RecordCommentRepository`가 추가된다.
- 기존 미구현 Swagger 표시는 실제 구현 명세로 교체된다.

## 9. 미결정 사항 (Open Questions)

- 댓글 수정/삭제는 정책 결정 후 후속 이슈에서 구현한다.
- 댓글 알림 발송은 알림 발송 아키텍처 이슈에서 분리한다.

## 10. 참고

- `docs/lld/LLD-0012-activity-record-create-api.md`
- `docs/lld/LLD-0013-activity-record-feed-api.md`
