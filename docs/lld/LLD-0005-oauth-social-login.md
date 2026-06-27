# LLD-0005: OAuth 소셜 로그인

> Low-Level Design. 이 문서는 OAuth 소셜 로그인 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Draft |
| Issue | #16 |
| 관련 ADR | ADR-0003 |
| 작성자 | msk226 |
| 작성일 | 2026-06-27 |

## 1. 목적 / 배경

- 카카오, 애플, 구글 소셜 로그인을 동일한 애플리케이션 흐름으로 처리한다.
- provider별 외부 API 차이는 전략 객체로 숨긴다.
- 회원 생성/조회와 JWT 발급은 공통 흐름으로 둔다.
- 로그인 응답에 `isNewMember`를 포함한다.
- 클라이언트는 `isNewMember` 값으로 온보딩 필요 여부를 판단한다.

## 2. 범위

### In scope

- 소셜 로그인 provider enum: `KAKAO`, `APPLE`, `GOOGLE`.
- provider별 OAuth 전략: access token 또는 authorization code로 외부 프로필을 조회한다.
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
- Apple private key 관리 방식 최종 확정.
- Google/Apple id token 검증 세부 구현.

## 3. 인터페이스 / API

### HTTP API

```http
POST /api/v1/auth/social-login
```

요청 예시:

```json
{
  "loginType": "KAKAO",
  "providerAccessToken": "provider-access-token"
}
```

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
    "isNewMember": true
  }
}
```

서버 callback 흐름이 필요하면 아래 API를 추가한다.

```http
GET /api/v1/oauth/{loginType}/redirect-url
GET /api/v1/oauth/{loginType}/callback?code={authorizationCode}
```

모바일 앱 우선 흐름은 `social-login` API로 본다.
클라이언트는 provider SDK로 받은 token을 서버에 전달한다.

### 내부 포트

```java
interface OAuthStrategy {
    LoginType getType();
    OAuthProfile getProfileByAccessToken(String accessToken);
    OAuthProfile getProfileByAuthorizationCode(String code);
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

1. 클라이언트가 `loginType`과 provider token을 전달한다.
2. `OAuthService`가 `OAuthStrategyFactory`에서 provider 전략을 선택한다.
3. 전략이 외부 provider API 또는 id token 검증으로 `OAuthProfile`을 만든다.
4. `OAuthMemberService`가 `loginType + providerUserId`로 기존 소셜 계정을 조회한다.
5. 기존 계정이 있으면 연결된 `memberId`와 `isNewMember=false`를 반환한다.
6. 기존 계정이 없으면 `member`와 `social_account`를 생성하고 `isNewMember=true`를 반환한다.
7. `TokenProvider`가 access/refresh token을 발급한다.
8. 기존 refresh token을 비활성화 또는 삭제한 뒤 새 refresh token을 DB에 저장한다.
9. `TokenDto(id, accessToken, refreshToken, isNewMember)`를 반환한다.

## 6. 예외 / 에러 처리

- 지원하지 않는 `loginType` → `UNSUPPORTED_LOGIN_TYPE`.
- provider token 검증 실패 → `INVALID_OAUTH_TOKEN`.
- provider 프로필 조회 실패 → `OAUTH_PROFILE_REQUEST_FAILED`.
- provider 고유 id 누락 → `INVALID_OAUTH_PROFILE`.
- refresh token 저장 실패 → 공통 서버 오류.

에러 코드는 구현 시 `ErrorStatus`에 추가하고 `GlobalExceptionHandler`의 공통 응답 구조를 따른다.

## 7. 인수조건 (Acceptance Criteria)

- [ ] provider별 전략이 `LoginType`으로 선택된다.
- [ ] 신규 소셜 계정 로그인 시 회원과 소셜 계정이 생성된다.
- [ ] 기존 소셜 계정 로그인 시 기존 회원을 반환한다.
- [ ] 로그인 성공 시 access token과 refresh token이 함께 발급된다.
- [ ] refresh token은 DB에 저장되고 회원별 기존 token은 교체된다.
- [ ] 응답에 `isNewMember`가 포함된다.
- [ ] provider 고유 id가 회원 매칭 기준으로 사용된다.
- [ ] `./gradlew build`가 통과한다.

## 8. 영향 범위 / 마이그레이션

- `auth` 패키지에 OAuth service, strategy, infrastructure client가 추가된다.
- `member` 패키지에 소셜 계정 기반 회원 생성/조회 service 또는 repository가 추가된다.
- `refresh_token` 테이블과 repository가 필요하다.
- Swagger 명세의 소셜 로그인 API는 본 LLD 기준으로 조정한다.

## 9. 미결정 사항 (Open Questions)

> ⚠️ 결정되지 않은 항목.
> PR 본문에서 빈칸으로 처리되는 확인 대상이며, 추측으로 채우지 말 것.

- [ ] 카카오/애플/구글을 모두 1차 출시 범위에 포함할지.
- [ ] 카카오 외 provider에서 서버가 받을 값이 access token인지 id token인지.
- [ ] 이메일이 없는 provider 프로필을 허용할지.
- [ ] 소셜 프로필 닉네임/이미지를 온보딩 전 임시값으로 저장할지.
- [ ] 서버 redirect/callback API를 운영에서 실제 사용할지, 문서/테스트용으로만 둘지.
- [ ] refresh token rotation 시 기존 row를 soft delete할지 같은 row를 갱신할지.

## 10. 참고

- provider별 구현은 전략 패턴으로 분리한다.
- 회원 매칭은 이메일이 아니라 `loginType + providerUserId` 기준이다.
- 외부 provider API 차이는 infrastructure client와 DTO 안에 가둔다.
