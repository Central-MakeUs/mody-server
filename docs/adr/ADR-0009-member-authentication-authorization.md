# ADR-0009: 회원 인증/인가 전략

> Architecture Decision Record. 하나의 중요한 의사결정과 그 이유를 기록한다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-06-29 |
| 관련 | LLD-0005, LLD-0010 |

## 맥락 (Context)

모디는 소셜 로그인 기반 모바일 서비스다.
클라이언트는 카카오, 구글, 애플 등 공급자 토큰을 사용해 서버에 로그인하고,
서버는 내부 회원을 식별한 뒤 자체 API 호출에 사용할 토큰을 발급해야 한다.

초기 운영에서는 Redis를 필수 인프라로 두지 않고 MySQL 중심으로 단순하게 시작한다.
동시에 로그아웃, 토큰 재발급, 탈퇴 이후 접근 차단을 위해 refresh token 상태는 서버가 추적해야 한다.

## 결정 (Decision)

인증은 서버 발급 JWT access token과 DB 저장 refresh token 조합으로 처리한다.

- 클라이언트는 보호 API 호출 시 `Authorization: Bearer <accessToken>`을 전달한다.
- access token은 짧은 만료 시간을 가진 stateless JWT로 사용한다.
- refresh token은 DB에 저장하고 재발급, 로그아웃, 탈퇴 시 서버에서 무효화한다.
- 재발급 시 refresh token을 검증하고 필요하면 rotation한다.
- 컨트롤러는 `@CurrentMember`로 인증된 회원 식별자를 주입받는다.
- 그룹, 기록, 마이페이지 접근 권한은 서비스 계층에서 회원 id와 리소스 소유/참여 관계로 검증한다.
- 소셜 공급자 토큰은 로그인 시 내부 회원 식별에만 사용하고, 서비스 API 인증에는 사용하지 않는다.

## 고려한 대안 (Considered Options)

1. **access token만 사용** — 구현은 단순하지만 로그아웃과 강제 만료 대응이 어렵다.
2. **Redis refresh token 저장** — TTL 관리가 편하지만 초기 인프라와 장애 지점이 늘어난다.
3. **DB refresh token 저장** — 조회 비용은 있지만 현재 인프라 안에서 무효화와 추적이 가능하다.

## 결과 (Consequences)

### 긍정

- API 인증 방식이 소셜 공급자와 분리된다.
- 로그아웃과 회원 탈퇴 시 refresh token을 서버에서 무효화할 수 있다.
- `@CurrentMember`를 통해 컨트롤러 인증 처리 중복을 줄인다.
- 그룹 단위 권한 검증을 서비스 유스케이스 안에서 명시적으로 처리할 수 있다.

### 부정 / 트레이드오프

- refresh token 검증 시 DB 접근이 필요하다.
- access token은 만료 전까지 stateless 특성상 즉시 폐기하기 어렵다.
- 권한 검증을 서비스 계층에서 빠뜨리면 데이터 노출 위험이 있으므로 테스트와 Swagger 예외 명세가 필요하다.

## 후속 / 미결정

- refresh token 재사용 감지 정책과 기기별 세션 관리 여부를 확정한다.
- 관리자 권한이나 그룹 역할이 생기면 권한 모델을 별도 ADR로 분리한다.
