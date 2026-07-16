# LLD-0018: 알림 조회 및 읽음 처리 API

> Low-Level Design. 이 문서는 알림 API 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #47 |
| 관련 ADR | ADR-0009 |
| 작성자 | Codex |
| 작성일 | 2026-07-04 |

## 1. 목적 / 배경

앱에서 댓글, 챌린지, 식사/운동 알림을 조회하고 읽음 처리할 수 있어야 한다.
이번 범위는 이미 생성된 알림 데이터를 조회/수정하는 API에 한정하며,
푸시 발송과 알림 생성 트리거는 별도 이슈에서 다룬다.

## 2. 범위

### In scope

- 내 알림 목록 조회.
- 내 알림 읽음 처리.
- 알림 타입, 상태, 생성일 기준 응답.
- 다른 회원 알림 접근 차단.
- Swagger 성공/주요 예외 응답 명세.

### Out of scope

- 알림 생성.
- 실제 푸시 발송.
- 발송 실패 재시도.
- 알림 전체 읽음 처리.
- 알림 삭제.

## 3. 인터페이스 / API

모든 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
GET /api/v1/notifications?cursor=100&size=20
PATCH /api/v1/notifications/{notificationId}/read
```

목록 응답:

```json
{
  "notifications": [
    {
      "notificationId": 1,
      "type": "COMMENT_CREATED",
      "title": "예은님이 댓글을 남겼어요.",
      "description": "어떤 이야기를 남겼는지 확인하러 가요!",
      "createdAt": "2026-07-04T10:00:00",
      "read": false
    }
  ],
  "nextCursor": null,
  "hasNext": false
}
```

## 4. 데이터 모델

- `notification`
  - `receiver_member_id`: 수신 회원 id.
  - `notification_type`: 알림 종류.
  - `notification_status`: `PENDING`, `SENT`, `READ`, `FAILED`.
  - `title`, `content`.
  - `read_at`.

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

### 알림 목록 조회

1. `@CurrentMember`가 access token에서 회원 id를 추출한다.
2. 활성 회원인지 검증한다.
3. `receiver_member_id`가 현재 회원이고 `deletedAt is null`인 알림을 조회한다.
4. `id desc` 커서 기반으로 `size + 1`개를 조회한다.
5. `notification_status == READ`이면 `read=true`로 응답한다.
6. 다음 페이지가 있으면 마지막 알림 id를 `nextCursor`로 응답한다.

### 알림 읽음 처리

1. 활성 회원인지 검증한다.
2. 알림 id로 활성 알림을 조회한다.
3. 알림 수신자가 현재 회원이 아니면 알림 없음과 동일하게 처리한다.
4. 알림 상태를 `READ`로 변경하고 `read_at`을 기록한다.
5. 이미 읽은 알림이면 상태를 유지하고 성공 응답한다.

## 6. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 회원 없음: `MEMBER302`.
- 알림 없음 또는 다른 회원 알림 접근: `NOTIFICATION302`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 인증 회원이 자신의 알림 목록을 조회할 수 있다.
- [x] 인증 회원이 자신의 알림을 읽음 처리할 수 있다.
- [x] 다른 회원 알림은 조회/수정할 수 없다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.
- [x] 관련 테스트가 통과한다.

## 8. 영향 범위 / 마이그레이션

- 기존 알림 API stub이 DB 기반 구현으로 교체된다.
- 알림 생성 및 발송 구조는 #52에서 별도로 설계한다.

## 9. 미결정 사항 (Open Questions)

- 알림 보관 기간과 전체 읽음 처리 제공 여부는 추후 정책 확정이 필요하다.
