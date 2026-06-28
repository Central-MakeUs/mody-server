# LLD-0008: 그룹 API

> Low-Level Design. 이 문서는 그룹 API 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Draft |
| Issue | #30 |
| 관련 ADR | ADR-0003 |
| 작성자 | Codex |
| 작성일 | 2026-06-28 |

## 1. 목적 / 배경

클라이언트가 그룹을 생성하고, 코드로 참여하고, 내가 속한 그룹과 구성원을 조회할 수 있어야 한다.
랜덤 그룹 코드는 서버가 생성해 중복 사용을 피한다.

## 2. 범위

### In scope

- 인증 회원 기준 그룹 코드 생성, 그룹 생성, 그룹 참여, 내 그룹 목록 조회, 그룹 구성원 조회, 그룹 나가기.
- 회원당 참여 그룹 최대 4개 제한.
- 그룹 나가기는 `GroupMember` 논리 삭제로 처리.

### Out of scope

- 그룹 초대 알림, 그룹 대표 선택, 그룹 삭제, 그룹별 공개 범위 세분화.
- 그룹 최대 인원 제한.

## 3. 인터페이스 / API

모든 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
GET /api/v1/groups/code
POST /api/v1/groups
POST /api/v1/groups/join
GET /api/v1/groups
GET /api/v1/groups/{groupId}/members
DELETE /api/v1/groups/{groupId}/members/me
```

요청 예시:

```json
{
  "name": "모디 그룹"
}
```

```json
{
  "code": "ABC123"
}
```

## 4. 데이터 모델

- `mody_group`
  - `id`: 내부 그룹 id.
  - `code`: 6자리 대문자/숫자 그룹 코드.
  - `name`: 그룹명, 최대 30자.
- `group_member`
  - `member_id`: 회원 id 논리 참조.
  - `group_id`: 그룹 id 논리 참조.
  - `group_member_status`: `JOINED`, `LEFT`.
  - `display_nickname`, `display_profile_image_key`: 그룹 내 노출용 회원 정보 스냅샷.
  - `joined_at`, `left_at`: 그룹 참여/탈퇴 시각.

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

1. `@CurrentMember`가 access token에서 회원 id를 추출한다.
2. 생성/참여 API는 회원 존재 여부와 참여 그룹 수 4개 제한을 확인한다.
3. 그룹 코드는 6자리 대문자/숫자로 생성하고, 활성 그룹 코드와 중복되면 재시도한다.
4. 그룹 생성 시 `mody_group`을 저장하고 생성자를 `group_member`로 함께 저장한다.
5. 그룹 참여 시 코드로 그룹을 찾고, 이미 참여 중인지 확인한 뒤 `group_member`를 저장한다.
6. 그룹 나가기는 `group_member_status=LEFT`, `left_at`, `deleted_at`, `status=INACTIVE`로 처리한다.

## 6. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 요청 검증 실패: `GROUP301`.
- 회원 없음: `MEMBER302`.
- 그룹 없음: `GROUP302`.
- 그룹 코드 생성 실패: `GROUP303`.
- 참여 가능 그룹 수 초과: `GROUP304`.
- 이미 참여 중인 그룹: `GROUP305`.
- 그룹 참여 정보 없음: `GROUP306`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 인증 회원이 랜덤 그룹 코드를 발급받을 수 있다.
- [x] 인증 회원이 그룹을 생성하면 생성자가 그룹 구성원으로 저장된다.
- [x] 인증 회원이 그룹 코드로 그룹에 참여할 수 있다.
- [x] 인증 회원이 참여 중인 그룹 목록과 구성원 목록을 조회할 수 있다.
- [x] 인증 회원이 그룹에서 나갈 수 있다.
- [x] 회원당 참여 그룹은 최대 4개로 제한된다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.
- [x] `./gradlew build`가 통과한다.

## 8. 영향 범위 / 마이그레이션

- `grouping` 패키지에 service/repository가 추가된다.
- 기존 그룹 API stub 응답은 DB 기반 구현으로 교체된다.
- 새 테이블은 기존 엔티티 `mody_group`, `group_member`를 사용한다.

## 9. 미결정 사항 (Open Questions)

- 없음.

## 10. 참고

- `docs/erd/ERD-0001-initial-domain-model.md`
