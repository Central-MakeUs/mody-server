# LLD-0029: 500 에러 Slack 알림

> Low-Level Design. 이 문서는 500 에러 Slack 알림 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #92 |
| 관련 ADR | ADR-0020 |
| 작성자 | Codex |
| 작성일 | 2026-07-12 |

## 1. 목적 / 배경

예상치 못한 500 계열 서버 오류를 Slack으로 전송해 운영 중 장애를 빠르게 인지한다.
알림은 API 응답 흐름과 분리하고, 민감 정보가 포함될 수 있는 요청 body는 전송하지 않는다.

## 2. 범위

### In scope

- 전역 예외 처리에서 500 계열 오류 알림 요청.
- Slack webhook 기반 알림 전송.
- 요청자 `memberId`, `nickname` 추출.
- 요청 method, URI, query, IP, User-Agent, exception 요약 포함.
- Slack 설정 미존재/비활성화 시 no-op.
- `local`, `dev` 프로필 전용 500 에러 테스트 API.

### Out of scope

- 요청 body 수집.
- Slack 알림 dedupe/rate limit.
- APM, 로그 수집기, 알림 대시보드 연동.
- 4xx 비즈니스/검증 오류 알림.

## 3. 인터페이스 / API

외부 API는 추가하지 않는다.

개발/검증 전용 API:

```http
POST /api/v1/dev/errors/internal-server-error
```

- `local`, `dev` 프로필에서만 활성화된다.
- Slack 알림 연동 확인을 위해 항상 `IllegalStateException`을 발생시킨다.
- Swagger 문서에는 노출하지 않는다.

환경 변수:

- `SLACK_ERROR_ALERT_ENABLED`: Slack 500 에러 알림 활성화 여부. 기본값 `false`.
- `SLACK_ERROR_ALERT_WEBHOOK_URL`: Slack incoming webhook URL. 기본값 빈 문자열.
- `SLACK_ERROR_ALERT_MAX_MESSAGE_LENGTH`: Slack 메시지 최대 길이. 기본값 `3500`.
- `SLACK_ERROR_ALERT_STACK_TRACE_DEPTH`: 포함할 stack trace line 수. 기본값 `8`.

## 4. 데이터 모델

DB 테이블은 추가하지 않는다.

내부 DTO:

- `ServerErrorAlert`
  - HTTP status, error code
  - method, URI, query, client IP, User-Agent
  - memberId, nickname
  - exception class, message, stack trace

## 5. 처리 흐름

1. `GlobalExceptionHandler`가 예상치 못한 예외 또는 500 계열 `GeneralException`을 처리한다.
2. 기존 API 실패 응답은 유지한다.
3. `ServerErrorAlertService`에 비동기 Slack 알림을 요청한다.
4. `ServerErrorAlertService`는 Authorization header에서 access token을 추출한다.
5. token 검증이 가능하면 `memberId`를 얻고, 회원 조회가 가능하면 `nickname`을 포함한다.
6. token이 없거나 검증에 실패하면 회원 정보는 `unknown`으로 남긴다.
7. Slack webhook client가 메시지를 전송한다.

## 6. 예외 / 에러 처리

- Slack webhook URL이 없거나 알림이 비활성화되어 있으면 전송하지 않는다.
- Slack 전송 실패는 warn 로그만 남기고 API 응답에 영향을 주지 않는다.
- 회원 정보 조회 실패도 알림 자체를 중단하지 않고 `unknown`으로 처리한다.
- query string의 `token`, `code`, `secret`, `password` 계열 값은 마스킹한다.

## 7. 인수조건 (Acceptance Criteria)

- [x] 예상치 못한 500 오류 발생 시 Slack 알림 요청이 수행된다.
- [x] Slack 메시지에 가능한 경우 `memberId`, `nickname`이 포함된다.
- [x] 요청 body와 Authorization 값은 Slack 메시지에 포함되지 않는다.
- [x] Slack 설정이 없으면 전송하지 않고 서버는 정상 기동한다.
- [x] Slack 전송 실패가 API 응답을 바꾸지 않는다.
- [x] 개발/검증용 500 에러 API로 Slack 알림 동작을 확인할 수 있다.
- [x] 관련 테스트와 `./gradlew build`가 통과한다.

## 8. 영향 범위 / 마이그레이션

- `GlobalExceptionHandler` 생성자에 선택적 alert service 의존성이 추가된다.
- Slack 알림 설정 환경 변수가 추가된다.
- DB 마이그레이션은 없다.

## 9. 미결정 사항 (Open Questions)

- 없음.

## 10. 참고

- `docs/adr/ADR-0020-server-error-slack-alert.md`
