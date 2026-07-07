# LLD-0027: 카카오 공유 초대 링크

> Low-Level Design. 이 문서는 #81 구현과 PR 본문의 기준이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #81 |
| 관련 ADR | 없음 |
| 작성자 | Codex |
| 작성일 | 2026-07-07 |

## 1. 목적 / 배경

카카오톡 공유하기로 `https://dev-mody.store/invite?code={inviteCode}` 링크를 전달한다.
iOS Universal Link 처리를 위해 AASA 파일을 제공하고, 앱 미설치 사용자가 링크를 열었을 때 최소 랜딩 페이지를 보여준다.

## 2. 범위

### In scope

- `/.well-known/apple-app-site-association` 응답 제공.
- AASA 응답의 `Content-Type: application/json`, `200 OK`, redirect 없음 보장.
- `/invite?code={inviteCode}` HTML 랜딩 페이지 제공.
- 출시 전 기본 목적지로 App Store 소개 페이지 자동 이동 및 수동 이동 버튼 제공.

### Out of scope

- 카카오 메시지 API 서버 발송.
- 앱 설치 여부 서버 판별.
- 실제 앱스토어 앱 상세 URL 확정.
- invite code 유효성 검증 및 그룹 가입 처리.

## 3. 설계

### AASA 제공

컨트롤러가 classpath의 `universal-link/apple-app-site-association` 리소스를 읽어 반환한다.
정적 리소스에 맡기지 않고 컨트롤러로 제공하는 이유는 확장자가 없는 AASA 파일의 `Content-Type`을 명시적으로 보장하기 위해서다.

제공 경로:

```http
GET /.well-known/apple-app-site-association
```

응답 조건:

- status: `200 OK`
- content type: `application/json`
- redirect: 없음

### Invite 랜딩 페이지

제공 경로:

```http
GET /invite?code={inviteCode}
```

동작:

1. 서버는 HTML을 반환한다.
2. 페이지는 300ms 후 `invite.app-store-url`로 이동을 시도한다.
3. `<meta refresh>`도 함께 제공해 JavaScript 비활성 환경을 보조한다.
4. 사용자가 직접 이동할 수 있도록 App Store 버튼을 제공한다.
5. `code`는 화면에 표시만 하며 서버에서 검증하지 않는다.

## 4. 설정

```yaml
invite:
  app-store-url: ${MODY_INVITE_APP_STORE_URL:https://www.apple.com/kr/app-store/}
```

출시 전에는 기본값을 사용하고, 출시 후 실제 앱 상세 URL이 확정되면 환경변수로 교체한다.

## 5. 테스트 시나리오

- AASA 경로가 200 OK, `application/json`, redirect 없음으로 응답한다.
- AASA에 dev 앱 id와 `/invite`, `/invite/*` 경로가 포함된다.
- `/invite?code=ABC123`가 HTML을 반환하고 코드, 자동 이동 URL, 수동 이동 버튼을 포함한다.
