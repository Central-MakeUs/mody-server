# LLD-0026: 개발 편의 API

> Low-Level Design. 이 문서는 #79 구현과 PR 본문의 오라클이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #79 |
| 관련 ADR | ADR-0012 |
| 작성자 | Codex |
| 작성일 | 2026-07-06 |

## 1. 목적 / 배경

클라이언트 개발 중 FCM 토큰, 알림함, 앱 진입 상태, 테스트 계정/그룹 정보를 빠르게 확인할 수 있는 개발 전용 API가 필요하다.
운영 노출 위험을 막기 위해 `local`, `dev` 프로필에서만 `/api/v1/dev/**` 컨트롤러를 활성화한다.
클라이언트 개발 편의성을 위해 dev API 자체에는 인증을 요구하지 않고, 대상 회원이 필요한 API는 `memberId`를 명시적으로 받는다.

## 2. 범위

### In scope

- 임의 FCM 토큰으로 테스트 푸시 즉시 발송.
- 지정한 회원의 알림함에 테스트 알림 생성.
- 지정한 회원의 앱 진입 상태 조회.
- mock 회원 생성 및 회원 목록 조회.
- 회원 id 기반 access/refresh token 발급.
- 그룹 id, 그룹명, 그룹 코드, 구성원 조회.
- Swagger 성공/예외 응답 문서화.

### Out of scope

- 운영 프로필 노출.
- 관리자 인증 체계.
- 테스트 데이터 전체 초기화 API.
- 실제 소셜 OAuth 공급자 호출.

## 3. API 동작

### Mock 회원 생성

```http
POST /api/v1/dev/members/mock
```

1. `nickname`이 없으면 `mock-{memberId suffix}` 형태로 자동 생성한다.
2. `personalInfoCompleted`가 생략되면 기본값은 `true`다.
3. 개인 정보 완료 상태이면 `birthDate`, `targetWeightKg`가 없을 때 기본값을 채운다.
4. `groupOnboardingCompleted`를 `true`로 주면 그룹 온보딩 완료 상태도 함께 만든다.

### 회원 목록 조회

```http
GET /api/v1/dev/members
```

1. 활성 회원만 반환한다.
2. 회원 id, 닉네임, 개인 정보 완료 여부, 그룹 온보딩 완료 여부, 참여 그룹 수를 내려준다.

### 회원 id 기반 토큰 발급

```http
POST /api/v1/dev/auth/tokens
```

1. 요청한 `memberId`가 활성 회원인지 확인한다.
2. 기존 `TokenProvider`로 access/refresh token을 발급한다.
3. refresh token은 기존 `RefreshTokenService.replace()`를 통해 DB에 저장한다.
4. 응답에 `personalInfoCompleted`, `groupOnboardingCompleted`, `mainAccessible`을 포함한다.

### FCM 테스트 푸시 발송

```http
POST /api/v1/dev/notifications/test-push
```

1. 요청 바디의 `fcmToken`, `title`, `body`를 검증한다.
2. DB에 알림을 저장하지 않고 `PushNotificationClient`로 즉시 발송한다.
3. `fcm.enabled=false`이면 No-Op 클라이언트가 사용되므로 실제 발송은 일어나지 않는다.
4. 응답에 `fcmEnabled`, `invalidToken`을 포함해 현재 설정과 토큰 오류 여부를 확인할 수 있게 한다.

### 알림함 테스트 알림 생성

```http
POST /api/v1/dev/notifications/inbox-test
```

1. 요청 바디의 `memberId`가 활성 회원인지 확인한다.
2. 해당 회원을 수신자로 하는 `DEV_TEST` 알림을 DB에 저장한다.
3. 저장된 알림은 기존 알림 조회 API에서 확인할 수 있다.

### 회원 앱 상태 조회

```http
GET /api/v1/dev/members/{memberId}/state
```

1. path의 `memberId`가 활성 회원인지 확인한다.
2. `personalInfoCompleted`, `groupOnboardingCompleted`, `mainAccessible`, `joinedGroupCount`를 계산한다.
3. 해당 회원이 참여 중인 그룹 목록을 함께 반환한다.

### 그룹 목록 / 상세 조회

```http
GET /api/v1/dev/groups
GET /api/v1/dev/groups/{groupId}
```

1. 활성 그룹만 반환한다.
2. 목록에서는 `groupId`, `name`, `code`, `memberCount`를 내려준다.
3. 상세에서는 그룹 구성원의 `memberId`, 그룹 내 표시 닉네임, 프로필 이미지 key, 참여 일시를 함께 내려준다.

## 4. 테스트 시나리오

- mock 회원을 생성한다.
- 회원 목록을 조회한다.
- 회원 id로 토큰을 발급하고 refresh token을 저장한다.
- FCM 토큰으로 테스트 푸시 발송 결과를 반환한다.
- 지정한 회원에게 테스트 알림을 생성한다.
- 지정한 회원의 앱 진입 상태와 그룹 목록을 반환한다.
- 그룹 목록과 그룹 상세를 조회한다.
- 회원 없음, 그룹 없음, 입력값 검증 실패 예외 응답이 Swagger에 생성된다.

## 5. 미결정 사항 (Open Questions)

- 운영 환경에서 유사 기능이 필요해지면 별도 관리자 인증/권한 체계를 먼저 설계한다.
