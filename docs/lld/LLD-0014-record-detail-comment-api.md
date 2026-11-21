# LLD-0014: 기록 상세 및 댓글 API

> Low-Level Design. 이 문서는 기록 상세 및 댓글 API 구현과 PR 본문의 **오라클(ground truth)** 이다.

> 최신 정책: 기록 상세와 댓글은 `groupId + recordId` 컨텍스트로 접근하며, 댓글은 그룹별로 분리된다.
> 현재 구현 기준은 `docs/lld/LLD-0031-record-upload-all-groups.md`를 따른다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #43, #62 |
| 관련 ADR | ADR-0007 |
| 작성자 | Codex |
| 작성일 | 2026-07-02 |

## 1. 목적 / 배경

기록 상세 화면에서 그룹원이 특정 회원의 같은 날짜 식사/운동 기록을 좌우 스와이프로 탐색하고,
댓글은 별도 API로 페이징 조회할 수 있게 한다.

## 2. 범위

### In scope

- 기록 상세 캐러셀 조회.
- 기록 댓글 커서 페이징 조회.
- 기록 댓글 작성.
- 기록/그룹 접근 권한 검증.
- 현재 그룹에서 나간 회원의 기록과 댓글 제외.
- 댓글 작성자 프로필 이미지와 내 댓글 여부 제공.
- Swagger 성공/예외 응답 명세 보강.

### Out of scope

- 댓글 수정/삭제.
- 댓글 알림 발송.
- 이미지 read signed URL 발급.

## 3. 인터페이스 / API

모든 구현 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
GET /api/v1/records/{recordId}
GET /api/v1/records/{recordId}/comments?cursor=10&size=20
POST /api/v1/records/{recordId}/comments
```

상세 응답은 기준 `recordId`와 같은 작성자, 같은 날짜, 같은 그룹의 기록을 `uploadedAt asc, id asc`로 반환한다.
`currentIndex`는 `records` 배열에서 기준 기록의 0-based 위치다.
클라이언트는 `totalCount`로 닷 인디케이터 개수를 표시한다.

댓글 목록 응답은 오래된 댓글부터 `id asc`로 반환한다.
다음 페이지는 `cursor={nextCursor}`로 요청한다.

댓글 작성 요청:

```json
{
  "content": "좋다"
}
```

응원 이모지 댓글 요청:

```json
{
  "content": ":mody-clap:"
}
```

텍스트 사이 이모지 댓글 요청:

```json
{
  "content": "오늘도 :mody-fire: 화이팅 :mody-clap:"
}
```

- 요청은 `content` 하나만 받는다. 비었거나 500자를 넘으면 `RECORD309`.
  - #277에서 만든 요청 `emojiId` 필드는 #282에서 뺐다. 운영에서는 테스트 댓글 5건 말고 쓰인 적이 없었다.
- 이모지 id는 `clap`, `medal`, `fire`, `heart`, `run`이다. 이미지는 앱에 두고 서버는 id만 저장한다.
- 이모지 댓글의 `content`는 빈 문자열로, id는 `emoji_id`에 저장한다.
- 텍스트 사이 이모지는 `content`에 `:mody-id:` 토큰으로 넣는다. 서버는 내용을 그대로 저장하고 그대로 내려준다.
  - 길이 제한은 500자(V26)이고 토큰 글자를 포함해 센다.
  - 지원하지 않는 토큰(예: `:mody-smile:`)은 막지 않고 일반 텍스트로 둔다. 앱은 아는 id만 이미지로 바꾼다.
- 토큰은 `mody-` 접두어를 붙인다. `:clap:`처럼 접두어 없는 표준 이모지 이름과 겹치지 않게 하기 위해서다. `emojiId` 값은 접두어 없이 `clap`이다.
- `content`가 지원하는 토큰 하나뿐이면(예: `":mody-clap:"`) 이모지 댓글로 저장한다.
- 댓글 목록 응답에 `emojiId`와 `emojiOnly`를 내려준다.
  - 이모지만 있는 댓글: `content` 빈 문자열, `emojiId` 값, `emojiOnly` true. 앱은 이 경우만 크게 그린다.
  - 그 외 댓글: `emojiId` null, `emojiOnly` false. `content`의 토큰을 이미지로 바꿔 그린다.
  - 이전 버전 앱은 섞인 댓글의 토큰을 글자 그대로 보여준다.

댓글 신고 (#286):

```http
POST /api/v1/groups/{groupId}/records/{recordId}/comments/{commentId}/report
```

- 응답은 `201`, `{"reportId", "commentId"}`. 신고 사유는 받지 않는다.
- 댓글 목록에 보이는 댓글만 신고할 수 있다. 삭제됐거나, 다른 기록·그룹의 댓글이거나, 작성자가 그룹을 나간 댓글은 `RECORD310`.
- 요청자는 그룹에 참여 중이어야 하고 기록도 그룹에서 보여야 한다(`GROUP306`, `RECORD302`). 기록 신고와 같은 기준이다.
- 같은 회원이 같은 댓글을 다시 신고하면 새로 만들지 않고 기존 신고를 돌려준다.
- 신고해도 댓글은 그대로 보인다. 관리자 처리(숨김·기각)는 아직 없다.

## 4. 데이터 모델

- `activity_record`
  - `id`, `group_id`, `member_id`, `record_type`, 기록 필드, `image_key`를 사용한다.
  - `deleted_at is null`인 기록만 조회한다.
- `record_comment`
  - `record_id`, `member_id`, `content`, `emoji_id`(이모지 댓글만, V25)를 저장한다.
  - `deleted_at is null`인 댓글만 조회한다.
- `comment_report` (V27)
  - `reporter_member_id`, `comment_id`, `record_id`, `group_id`, 처리 결과(`resolution`, `resolved_at`, `resolved_by`)를 저장한다.
  - 삭제되지 않은 신고는 회원·댓글 조합마다 하나만 둔다(유니크 인덱스).
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
6. 상세 응답에 기준 기록 작성자의 같은 날짜 기록 목록, 전체 개수, 현재 인덱스를 포함한다.
7. 상세 응답에는 댓글을 포함하지 않는다.
8. 댓글 목록 조회는 접근 가능한 기록에 대해 별도 커서 페이징으로 조회한다.
9. 댓글 작성자 프로필 이미지와 `isMine`을 함께 반환한다.
10. 댓글 작성은 접근 검증 후 Snowflake id를 발급해 `record_comment`에 저장한다.

개인 기록(`group_id is null`)은 작성자 본인만 상세 조회와 댓글 작성이 가능하다.

## 6. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 회원 없음: `MEMBER302`.
- 기록 없음 또는 노출 불가 기록: `RECORD302`.
- 그룹 없음: `GROUP302`.
- 그룹 참여 정보 없음: `GROUP306`.
- 댓글 입력값 검증 실패: `RECORD301`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 접근 가능한 기록 상세 캐러셀을 조회할 수 있다.
- [x] 상세 응답에 같은 작성자/같은 날짜의 기록 목록, 전체 개수, 현재 인덱스가 포함된다.
- [x] 댓글은 별도 API로 커서 페이징 조회할 수 있다.
- [x] 댓글 응답에 작성자 프로필 이미지와 내 댓글 여부가 포함된다.
- [x] 접근 가능한 기록에 댓글을 작성할 수 있다.
- [x] 삭제 처리된 기록/댓글과 현재 그룹원이 아닌 작성자의 댓글은 노출되지 않는다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.
- [x] 서비스 단위 테스트가 주요 분기를 검증한다.

## 8. 영향 범위 / 마이그레이션

- `ActivityRecordService`에 상세 조회와 댓글 작성 기능이 추가된다.
- `RecordCommentRepository`가 추가된다.
- 기존 상세 응답의 `comments` 필드는 제거되고 댓글 목록 API로 분리된다.

## 9. 미결정 사항 (Open Questions)

- 댓글 수정/삭제는 정책 결정 후 후속 이슈에서 구현한다.
- 댓글 알림 발송은 알림 발송 아키텍처 이슈에서 분리한다.
- 댓글 페이징의 기본 정렬은 오래된 순으로 둔다. 최신순 요구가 생기면 cursor 방향을 조정한다.

## 10. 참고

- `docs/lld/LLD-0012-activity-record-create-api.md`
- `docs/lld/LLD-0013-activity-record-feed-api.md`
