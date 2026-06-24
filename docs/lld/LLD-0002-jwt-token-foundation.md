# LLD-0002: JWT 토큰 기반 로직

> Low-Level Design. 이 문서는 해당 기능 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Approved |
| Issue | _(미발급 — GitHub Issue 생성 후 채움)_ |
| 관련 ADR | - |
| 작성자 | msk226 |
| 작성일 | 2026-06-23 |

## 1. 목적 / 배경
- 이후 로그인/인증 기능에서 재사용할 JWT access/refresh 토큰 발급 및 검증 기반이 필요하다.
- access/refresh token을 같은 포트로 발급하고 검증하는 구조를 mody의 단일 모듈 Java/Spring Boot 구조에 맞게 준비한다.

## 2. 범위
### In scope
- JWT 의존성: `jjwt-api`, `jjwt-impl`, `jjwt-jackson`.
- 설정 바인딩: `token.access-secret`, `token.access-token-expiration-time`, `token.refresh-token-expiration-time`.
- 토큰 포트: `TokenProvider`.
- 토큰 구현체: `JwtTokenProvider`.
- 토큰 응답 DTO: `TokenDto(id, accessToken, refreshToken, isNewMember)`.
- JWT 공통 상수: `Authorization`, `Bearer `.
- JWT 에러 코드: `EMPTY_JWT`, `INVALID_JWT`, `EXPIRED_JWT`, `UNSUPPORTED_JWT`, `NO_AUTHORIZED`, `INVALID_REFRESH_TOKEN`.
- access/refresh 토큰 생성, 검증, `memberId` 추출 테스트.

### Out of scope
- Spring Security 필터 체인 연결.
- 로그인/회원가입 API와 토큰 발급 연결.
- refresh token 저장소(DB/Redis)와 재발급 서비스.
- `@CurrentMember` 같은 인증 사용자 주입 어노테이션.
- OAuth provider 연동.

## 3. 인터페이스 / API
외부 HTTP API는 추가하지 않는다. 내부 포트는 다음과 같다.

```java
interface TokenProvider {
    TokenDto createToken(Long memberId);
    void validateToken(String token);
    Long getMemberIdByToken(String token);
}
```

`JwtTokenProvider`는 access token과 refresh token에 공통으로 다음 claim을 넣는다.
- `memberId`: 회원 식별자.
- `tokenType`: `access` 또는 `refresh`.

## 4. 데이터 모델
- 신규 테이블 없음.
- `TokenDto`
  - `id`: 토큰 대상 회원 ID.
  - `accessToken`: access JWT.
  - `refreshToken`: refresh JWT.
  - `isNewMember`: 신규 회원 여부. 기본값은 `false`.
- `JwtProperties`
  - `accessSecret`: HS256 서명 키. 최소 32 bytes 이상이어야 한다.
  - `accessTokenExpirationTime`: access token 만료 시간(ms).
  - `refreshTokenExpirationTime`: refresh token 만료 시간(ms).

## 5. 처리 흐름
- `createToken(memberId)`
  1. 현재 시각을 기준으로 access token을 생성한다.
  2. 같은 `memberId`로 refresh token을 생성한다.
  3. `TokenDto.of(memberId, accessToken, refreshToken)`를 반환한다.
- `validateToken(token)`
  1. blank token이면 `EMPTY_JWT`.
  2. JJWT parser로 서명과 만료 시간을 검증한다.
  3. 검증 실패 타입에 따라 `GeneralException(ErrorStatus.XXX)`로 변환한다.
- `getMemberIdByToken(token)`
  1. `validateToken`과 동일한 parser 경로로 claims를 읽는다.
  2. `memberId` claim을 `Long`으로 변환해 반환한다.

## 6. 예외 / 에러 처리
- blank token 또는 JJWT `IllegalArgumentException` → `EMPTY_JWT`.
- 만료된 토큰 → `EXPIRED_JWT`.
- 지원하지 않는 JWT 또는 서명 불일치 → `UNSUPPORTED_JWT`.
- 잘못된 JWT 형식 또는 claim 변환 실패 → `INVALID_JWT`.
- 모든 예외는 `GeneralException`으로 던지고 `GlobalExceptionHandler`가 공통 응답으로 변환한다.

## 7. 인수조건 (Acceptance Criteria)
- [x] access token과 refresh token이 함께 발급된다.
- [x] access token에서 `memberId`를 추출할 수 있다.
- [x] refresh token에서 `memberId`를 추출할 수 있다.
- [x] access/refresh 만료 토큰이 `EXPIRED_JWT`로 매핑된다.
- [x] access/refresh 서명 불일치 토큰이 `UNSUPPORTED_JWT`로 매핑된다.
- [x] blank token이 `EMPTY_JWT`로 매핑된다.
- [x] `./gradlew build`가 통과한다.

## 8. 영향 범위 / 마이그레이션
- 신규 패키지 `cmc.mody.auth`가 추가된다.
- `build.gradle.kts`에 validation 및 JWT 의존성이 추가된다.
- `application.yaml`, `application-local.yaml`, test `application.yaml`에 token 설정이 추가된다.
- 아직 Security 필터와 연결되지 않았으므로 기존 API 동작에는 인증 요구가 생기지 않는다.

## 9. 미결정 사항 (Open Questions)
> ⚠️ 결정되지 않은 항목. PR 본문에서 빈칸으로 처리되며 확인 대상이 된다. 추측으로 채우지 말 것.
- [ ] access token / refresh token 만료 시간 최종값.
- [ ] refresh token 저장소 선택(DB, Redis, 또는 다른 저장소).
- [ ] refresh token rotation 정책.
- [ ] 인증 실패 HTTP status를 JWT 세부 에러별로 모두 400으로 둘지, 401/403으로 조정할지.
- [ ] Spring Security 필터 적용 범위와 permitAll 경로.
- [ ] 인증 사용자 주입 방식(`@CurrentMember` 등) 도입 여부.

## 10. 참고
- JJWT 0.11.5의 HS256 서명과 parser 기반 검증을 사용한다.
- JWT 예외는 `GeneralException(ErrorStatus.XXX)`로 변환한다.
