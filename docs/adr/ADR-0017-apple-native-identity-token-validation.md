# ADR-0017: Apple 네이티브 identity token 검증

> Architecture Decision Record. 하나의 중요한 의사결정과 그 이유를 기록한다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-07-05 |
| 관련 | LLD-0005, Issue #69 |

## 맥락 (Context)

iOS 클라이언트는 Apple SDK로 로그인한 뒤 서버에 `identityToken`을 전달한다.
서버가 이 토큰의 payload만 디코딩하면 다음 문제가 생긴다.

- 공격자가 임의로 만든 JWT payload를 전달해도 `sub`를 신뢰할 수 있다.
- DEV/PROD Bundle ID가 섞여도 서버가 구분하지 못한다.
- 만료된 토큰이나 Apple이 발급하지 않은 토큰을 걸러내지 못한다.

Apple 로그인에서 공유받은 운영 식별자는 다음과 같다.

- Team ID: `BLRYMXGV5K`
- DEV Bundle ID: `com.jagsim.mody-dev`
- PROD Bundle ID: `com.jagsim.mody`

## 결정 (Decision)

클라이언트용 Apple 로그인은 authorization code 교환이 아니라 네이티브 `identityToken` 검증을 기준으로 처리한다.

검증 기준은 다음과 같다.

- `iss`는 `https://appleid.apple.com`이어야 한다.
- `aud`는 실행 환경의 Bundle ID여야 한다.
  - local/dev: `com.jagsim.mody-dev`
  - prod: `com.jagsim.mody`
- `exp`가 지나면 거부한다.
- JWT header의 `kid`와 Apple JWKS의 공개키를 매칭하고 RSA 서명을 검증한다.
- 검증된 `sub`를 `SocialAccount.providerUserId`로 사용한다.

Team ID는 identity token claim에 포함되지 않으므로 직접 검증 대상이 아니다.
대신 Apple Developer 계정 추적과 향후 authorization code/client secret 흐름 확장을 위해 설정으로 보관한다.

## 고려한 대안 (Considered Options)

1. **payload만 디코딩**  
   구현은 단순하지만 서명, 만료, audience를 검증하지 못해 보안상 부적절하다.
2. **Apple authorization code를 서버에서 교환**  
   client secret 생성을 위해 Key ID와 private key 운영이 필요하다. 현재 iOS 네이티브 플로우에는 과하다.
3. **identity token 자체 검증**  
   추가 구현은 필요하지만 네이티브 로그인 흐름과 맞고, 서버가 독립적으로 토큰 신뢰성을 판단할 수 있다.

## 결과 (Consequences)

### 긍정

- 서버가 Apple id token의 발급자, 대상, 만료, 서명을 직접 검증한다.
- DEV/PROD 앱 Bundle ID가 섞이는 문제를 환경 설정으로 방지한다.
- Apple private key 없이도 네이티브 로그인 API를 운영할 수 있다.

### 부정 / 트레이드오프

- Apple 공개키 조회와 `kid` 매칭 로직이 추가된다.
- 공개키 회전 대응을 위해 캐시 TTL과 재조회 정책을 유지해야 한다.
- 서버 callback 기반 Apple 로그인까지 완성하려면 별도 client secret 생성 전략이 필요하다.
