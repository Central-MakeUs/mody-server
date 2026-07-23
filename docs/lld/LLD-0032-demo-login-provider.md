# LLD-0032: 심사용 데모 로그인 Provider

> Low-Level Design. 이 문서는 앱 심사용 데모 로그인 provider 구현의 기준이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #125 |
| 관련 ADR | ADR-0005, ADR-0009 |
| 작성자 | msk226 |
| 작성일 | 2026-07-23 |

## 1. 목적 / 배경

앱 심사자는 Kakao/Apple/Google 로그인을 정상적으로 수행하기 어렵다.
Android와 iOS가 숨겨진 데모 로그인 플로우에서 서버에 `ANDROIDTEST`, `IOSTEST` provider를 전달하면,
서버는 각 플랫폼별 데모 회원 기준으로 서비스 JWT를 발급한다.

## 2. 범위

### In scope

- `LoginType`: `ANDROIDTEST`, `IOSTEST` 추가.
- 기존 클라이언트 소셜 로그인 API에서 데모 provider 허용.
- 외부 OAuth 호출 없이 고정 provider user id로 `OAuthProfile` 생성.
- `OAuthMemberProcessor`와 refresh token 저장 흐름 재사용.
- `DEMO_LOGIN_ENABLED` 서버 플래그로 데모 로그인 차단.
- Swagger에 데모 로그인 성공/차단 케이스 명시.

### Out of scope

- 심사 직전 데모 계정 데이터 초기화 자동화.
- 모바일의 숨겨진 데모 로그인 UI와 원격 Flag.
- 출시 후 운영 정책 확정.

## 3. 인터페이스 / API

```http
GET /api/v1/oauth/client/ANDROIDTEST
GET /api/v1/oauth/client/IOSTEST
```

데모 provider는 외부 provider token을 검증하지 않으므로 `accessToken` query parameter를 생략할 수 있다.
일반 provider는 기존처럼 `accessToken`을 필수로 받는다.

응답 구조는 기존 클라이언트 소셜 로그인과 동일하다.

## 4. 설정

```yaml
demo-login:
  enabled: ${DEMO_LOGIN_ENABLED:false}
  android-provider-user-id: ${DEMO_ANDROID_PROVIDER_USER_ID:mody-android-reviewer}
  ios-provider-user-id: ${DEMO_IOS_PROVIDER_USER_ID:mody-ios-reviewer}
  android-nickname: ${DEMO_ANDROID_NICKNAME:AndroidReviewer}
  ios-nickname: ${DEMO_IOS_NICKNAME:iOSReviewer}
```

운영 기본값은 비활성화다. 심사 배포에서만 `DEMO_LOGIN_ENABLED=true`를 주입한다.
provider user id는 `social_account.login_type + provider_user_id` 매칭 기준으로 사용되므로,
값을 변경하면 다른 데모 회원으로 인식된다.

## 5. 처리 흐름

1. 모바일이 숨겨진 데모 로그인 플로우에서 `ANDROIDTEST` 또는 `IOSTEST` provider로 API를 호출한다.
2. `LoginType.from()`이 provider 값을 enum으로 변환한다.
3. `OAuthStrategyFactory`가 플랫폼별 demo strategy를 선택한다.
4. demo strategy가 `DEMO_LOGIN_ENABLED`를 확인한다.
5. 비활성화 상태면 `DEMO_LOGIN_DISABLED`를 반환한다.
6. 활성화 상태면 설정된 provider user id와 닉네임으로 `OAuthProfile`을 만든다.
7. 이후 회원 생성/조회, 온보딩 상태 계산, access/refresh token 발급은 기존 OAuth 흐름을 그대로 사용한다.

## 6. 예외 / 에러 처리

- 지원하지 않는 `loginType` → `AUTH407`.
- 일반 provider에서 `accessToken` 누락 → `COMMON4000`.
- 데모 로그인 비활성화 → `AUTH411`.
- 데모 provider user id 설정 누락 → `AUTH410`.
- 활성 소셜 계정이 비활성 회원을 참조하는 비정상 데이터 → `MEMBER302`.

## 7. 인수조건 (Acceptance Criteria)

- [x] `ANDROIDTEST`, `IOSTEST` provider가 파싱된다.
- [x] 데모 provider는 `accessToken` 없이 호출할 수 있다.
- [x] 서버 플래그가 꺼져 있으면 데모 로그인이 차단된다.
- [x] 서버 플래그가 켜져 있으면 각 플랫폼별 데모 회원 기준으로 JWT가 발급된다.
- [x] 일반 provider의 `accessToken` 필수 정책은 유지된다.
- [x] Swagger에 데모 로그인 성공/차단 케이스가 표시된다.
- [x] `./gradlew test`가 통과한다.

## 8. 미결정 사항 (Open Questions)

- [ ] 심사 직전 데모 계정 초기 데이터 범위와 초기화 방식.
- [ ] 출시 후 서버 데모 로그인 차단을 항상 유지할지 여부.
