# LLD-0005: OAuth 소셜 로그인

> Low-Level Design. 이 문서는 OAuth 소셜 로그인 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Draft |
| Issue | #18 |
| 관련 ADR | ADR-0003 |
| 작성자 | msk226 |
| 작성일 | 2026-06-27 |

관련 상세 설계: LLD-0006.

## 1. 목적 / 배경

- 카카오, 애플, 구글 소셜 로그인을 동일한 애플리케이션 흐름으로 처리한다.
- provider별 외부 API 차이는 전략 객체로 숨긴다.
- 회원 생성/조회와 JWT 발급은 공통 흐름으로 둔다.
- 로그인 응답에 `personalInfoCompleted`를 포함한다.
- 클라이언트는 `personalInfoCompleted=false`이면 개인 정보 입력 화면으로 이동한다.

## 2. 범위

### In scope

- 소셜 로그인 provider enum: `KAKAO`, `APPLE`, `GOOGLE`.
- provider별 OAuth 전략: authorization code로 외부 프로필을 조회한다.
- provider HTTP client는 Spring Cloud OpenFeign 기반으로 구현한다.
- 전략 선택 팩토리: 등록된 전략을 `LoginType` 기준으로 조회한다.
- OAuth 프로필 표준 DTO: `loginType`, `providerUserId`, `email`, `nickname`, `profileImageUrl`.
- 회원 보장 흐름: 소셜 계정이 있으면 기존 회원을 반환한다.
- 소셜 계정이 없으면 회원과 소셜 계정을 생성한다.
- 토큰 발급: 기존 `TokenProvider`로 access/refresh token을 발급한다.
- refresh token 저장: 회원별 기존 refresh token을 비활성화 또는 교체 후 DB에 저장한다.

### Out of scope

- 실제 provider별 client id/secret 발급과 운영 콘솔 설정.
- Spring Security 필터 체인 적용.
- 회원 온보딩 상세 입력 API 구현.
- Spring Security 필터 체인 기반 인증 적용.
- Apple id token 공개키 서명 검증.

## 3. 인터페이스 / API

### HTTP API

```http
GET /api/v1/oauth/{loginType}/redirect-url
GET /api/v1/oauth/{loginType}/callback?code={authorizationCode}
POST /api/v1/oauth/{loginType}/callback
GET /api/v1/oauth/client/{loginType}?accessToken={providerToken}
```

`redirect-url`은 provider 로그인 화면으로 이동할 URL을 반환한다.
`callback`은 provider가 내려준 authorization code로 서비스 JWT를 발급한다.
Apple authorization redirect는 `response_mode=form_post`를 사용하므로 POST callback도 지원한다.
`client/{loginType}`은 클라이언트가 provider SDK로 받은 token을 서버 JWT로 교환한다.
Kakao/Google은 access token, Apple은 identity token을 `accessToken` query parameter로 전달한다.

응답 예시:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {
    "id": 1,
    "accessToken": "access.jwt",
    "refreshToken": "refresh.jwt",
    "personalInfoCompleted": false
  }
}
```

### 내부 포트

```java
interface OAuthStrategy {
    LoginType getType();
    OAuthProfile getProfileByAuthorizationCode(String code);
    OAuthProfile getProfileByProviderToken(String providerToken);
    String getRedirectUrl();
}
```

```java
interface OAuthMemberService {
    OAuthMemberResult ensure(OAuthProfile profile);
}
```

## 4. 데이터 모델

- `member`
  - 앱 내부 회원 식별자와 온보딩 기본 정보를 저장한다.
  - 소셜 로그인 직후 온보딩 전 상태를 허용하기 위해 생년월일은 nullable이다.
- `social_account`
  - `member_id`: 논리 참조 id.
  - `login_type`: `KAKAO`, `APPLE`, `GOOGLE`.
  - `provider_user_id`: provider에서 내려주는 고유 사용자 id.
- `refresh_token`
  - `member_id`: 논리 참조 id.
  - `token`: refresh JWT.
  - `status`, `deleted_at`: rotation 또는 로그아웃 시 비활성화에 사용한다.

`social_account` 조회 기준은 `login_type + provider_user_id`다.
이메일은 변경될 수 있으므로 회원 매칭의 주 식별자로 쓰지 않는다.

## 5. 처리 흐름

1. 서버 callback 흐름은 클라이언트가 redirect URL을 조회하고 provider 로그인을 진행한다.
2. `OAuthService`가 `OAuthStrategyFactory`에서 provider 전략을 선택한다.
3. 전략이 authorization code로 provider token을 교환하고 외부 프로필을 조회해 `OAuthProfile`을 만든다.
4. `OAuthMemberService`가 `loginType + providerUserId`로 기존 소셜 계정을 조회한다.
5. 기존 계정이 있으면 연결된 `memberId`와 개인 정보 입력 완료 여부를 반환한다.
6. 기존 계정이 없으면 `member`와 `social_account`를 생성하고 `personalInfoCompleted=false`를 반환한다.
7. `TokenProvider`가 access/refresh token을 발급한다.
8. 기존 refresh token을 비활성화 또는 삭제한 뒤 새 refresh token을 DB에 저장한다.
9. `TokenDto(id, accessToken, refreshToken, personalInfoCompleted)`를 반환한다.

클라이언트 provider token 흐름은 1~3단계 대신 `GET /api/v1/oauth/client/{loginType}`로 받은
provider token을 각 전략의 `getProfileByProviderToken`에 전달한다. 이후 회원 보장과 JWT 발급 흐름은 동일하다.

## 6. 예외 / 에러 처리

- 지원하지 않는 `loginType` → `UNSUPPORTED_LOGIN_TYPE`.
- 클라이언트용 API의 `accessToken` 누락 → `BAD_REQUEST`.
- provider token 교환 실패 → `INVALID_OAUTH_TOKEN`.
- provider 프로필 조회 실패 → `OAUTH_PROFILE_REQUEST_FAILED`.
- provider 고유 id 누락 → `INVALID_OAUTH_PROFILE`.
- refresh token 저장 실패 → 공통 서버 오류.

에러 코드는 구현 시 `ErrorStatus`에 추가하고 `GlobalExceptionHandler`의 공통 응답 구조를 따른다.

## 7. 인수조건 (Acceptance Criteria)

- [x] provider별 전략이 `LoginType`으로 선택된다.
- [x] 신규 소셜 계정 로그인 시 회원과 소셜 계정이 생성된다.
- [x] 기존 소셜 계정 로그인 시 기존 회원을 반환한다.
- [x] 로그인 성공 시 access token과 refresh token이 함께 발급된다.
- [x] refresh token은 DB에 저장되고 회원별 기존 token은 교체된다.
- [x] 응답에 `personalInfoCompleted`가 포함된다.
- [x] provider 고유 id가 회원 매칭 기준으로 사용된다.
- [x] `./gradlew build`가 통과한다.

## 8. 영향 범위 / 마이그레이션

- `auth` 패키지에 OAuth service, strategy, OpenFeign 기반 infrastructure client가 추가된다.
- `member` 패키지에 소셜 계정 기반 회원 생성/조회 repository가 추가된다.
- `refresh_token` 엔티티와 repository가 추가된다.
- `application.yaml`에 provider별 OAuth endpoint/client 설정이 추가된다.
- Swagger 명세의 소셜 로그인 API는 본 LLD 기준으로 조정한다.

## 9. 미결정 사항 (Open Questions)

> ⚠️ 결정되지 않은 항목.
> PR 본문에서 빈칸으로 처리되는 확인 대상이며, 추측으로 채우지 말 것.

- [ ] 이메일이 없는 provider 프로필을 허용할지.
- [ ] 소셜 프로필 닉네임/이미지를 온보딩 전 임시값으로 저장할지.
- [ ] Apple id token 서명 검증을 어느 단계에서 적용할지.
- [ ] Apple client secret 생성/회전 방식을 서버 내부 생성으로 바꿀지, 운영 환경변수 주입으로 둘지.

## 10. 참고

- provider별 구현은 전략 패턴으로 분리한다.
- 회원 매칭은 이메일이 아니라 `loginType + providerUserId` 기준이다.
- 외부 provider API 차이는 infrastructure client와 DTO 안에 가둔다.
