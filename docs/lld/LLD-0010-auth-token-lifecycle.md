# LLD-0010: 토큰 재발급 및 로그아웃 API

> Low-Level Design.
> 이 문서는 refresh token 기반 세션 관리 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #36 |
| 관련 ADR | ADR-0003 |
| 작성자 | Codex |
| 작성일 | 2026-06-29 |

## 1. 목적 / 배경

소셜 로그인 후 발급된 access token은 만료될 수 있으므로
refresh token으로 새 토큰을 발급해야 한다.
로그아웃 시에는 DB에 저장된 refresh token을 논리 삭제해 이후 재사용을 막고,
해당 회원의 활성 FCM token도 함께 비활성화해 로그아웃 이후 알림 수신을 중단한다.

## 2. 범위

### In scope

- 토큰 재발급.
- 로그아웃.
- refresh token DB 검증, 교체, 삭제.
- 로그아웃 시 회원의 활성 push token 비활성화.
- Swagger 성공/예외 응답 명세.

### Out of scope

- 다른 기기 세션별 로그아웃 정책.
- refresh token rotation 탐지 및 계정 보호 정책.
- Redis 기반 세션 저장소.

## 3. 인터페이스 / API

```http
POST /api/v1/auth/reissue
POST /api/v1/auth/logout
```

요청 본문:

```json
{
  "refreshToken": "refresh-token"
}
```

재발급 응답:

```json
{
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token"
}
```

## 4. 데이터 모델

- `refresh_token`
  - `member_id`, `token`을 저장한다.
  - 재발급 성공 시 기존 활성 refresh token은 논리 삭제하고 새 refresh token을 저장한다.
  - 로그아웃 시 요청 refresh token만 논리 삭제한다.
- `member_push_token`
  - 로그아웃 시 해당 회원의 활성 push token을 모두 비활성화한다.

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

### 토큰 재발급

1. 요청 본문의 refresh token이 비어 있으면 `AUTH406`을 반환한다.
2. JWT 서명/만료/type을 검증하고 refresh token에서 회원 id를 추출한다.
3. DB에서 동일한 활성 refresh token 목록을 조회한다. 중복 활성 토큰이 있으면 최신 1개만 사용하고 나머지는 논리 삭제한다.
4. 사용 대상 refresh token의 회원 id가 JWT의 회원 id와 일치하는지 확인한다.
5. 새 access/refresh token을 발급한다.
6. 기존 회원 활성 refresh token과 새 refresh token 값으로 남아 있는 활성 중복 토큰을 논리 삭제한 뒤 새 refresh token을 저장한다.
7. 새 access/refresh token을 반환한다.

### 로그아웃

1. 요청 본문의 refresh token이 비어 있으면 `AUTH406`을 반환한다.
2. JWT 서명/만료/type을 검증하고 refresh token에서 회원 id를 추출한다.
3. DB에서 동일한 활성 refresh token 목록을 조회한다. 중복 활성 토큰이 있으면 최신 1개만 사용하고 나머지는 논리 삭제한다.
4. 사용 대상 refresh token의 회원 id가 JWT의 회원 id와 일치하는지 확인한다.
5. 해당 refresh token을 논리 삭제한다.
6. 해당 회원의 활성 push token을 모두 비활성화한다.

## 6. 예외 / 에러 처리

- 만료된 refresh token: `AUTH404`.
- 지원하지 않는 JWT: `AUTH405`.
- 비어 있거나 저장되지 않았거나 access token이 전달된 refresh token: `AUTH406`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 저장된 refresh token으로 토큰을 재발급할 수 있다.
- [x] 재발급 성공 시 기존 refresh token은 비활성화되고 새 refresh token이 저장된다.
- [x] 로그아웃 시 요청 refresh token은 비활성화된다.
- [x] 로그아웃 시 해당 회원의 push token은 모두 비활성화된다.
- [x] 유효하지 않은 refresh token은 `AUTH406`으로 응답한다.
- [x] Swagger에 성공/예외 응답이 반영된다.
- [x] `./gradlew build`가 통과한다.

## 8. 영향 범위 / 마이그레이션

- `auth` 패키지에 세션 관리 service가 추가된다.
- `refresh_token` repository는 token 활성 목록 조회를 사용해 중복 활성 데이터로 인한 단건 조회 예외를 방지한다.
- `V4__deduplicate_active_refresh_tokens.sql`에서 기존 활성 refresh token 중복을 정리하고 token 조회 인덱스를 추가한다.
- 기존 stub Auth API가 DB 기반 구현으로 교체된다.

## 9. 미결정 사항 (Open Questions)

- 없음.
