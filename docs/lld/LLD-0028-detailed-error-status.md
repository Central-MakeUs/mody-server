# LLD-0028: 세부 에러 코드 매핑

> Low-Level Design. 이 문서는 API 입력 오류 코드 세분화 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #85 |
| 관련 ADR | ADR-0019 |
| 작성자 | Codex |
| 작성일 | 2026-07-11 |

## 1. 목적 / 배경

클라이언트가 실패 응답의 `code`와 `message`만 보고 요청값 오류 원인을 판단할 수 있도록,
넓은 validation 에러 코드를 화면 정책 단위 세부 코드로 변환한다.

## 2. 범위

### In scope

- 온보딩 개인 정보 입력 세부 오류 코드.
- 기록 생성 및 댓글 생성 세부 오류 코드.
- `MethodArgumentNotValidException`을 세부 `ErrorStatus`로 변환하는 resolver.
- Swagger 예외 응답 예시 보강.

### Out of scope

- 실패 응답 구조 변경.
- field error 목록 응답.
- 운영 알림/Slack 전송.
- 모든 API validation의 완전 세분화.

## 3. 세부 에러 코드

### 회원/온보딩

- `MEMBER304`: 닉네임 형식 오류.
- `MEMBER305`: 생년월일 오류.
- `MEMBER306`: 체중 범위 또는 소수점 자리 오류.
- `MEMBER307`: 식사 설정 개수 또는 식사 타입 조합 오류.
- `MEMBER308`: 식사 시간과 먹지 않음 조합 오류.
- `MEMBER309`: 운동 일정 개수, 요일, 시간 오류.

### 기록/댓글

- `RECORD303`: 그룹 id 형식 오류.
- `RECORD304`: 기록 타입 누락 또는 오류.
- `RECORD305`: 기록 이미지 키 누락, 길이, 도메인 오류.
- `RECORD306`: 식사 기록 payload 조합 오류.
- `RECORD307`: 운동 기록 payload 조합 오류.
- `RECORD308`: 운동 시간 범위 오류.
- `RECORD309`: 댓글 내용 오류.

## 4. 처리 흐름

1. Controller request body 검증 실패 시 `MethodArgumentNotValidException`이 발생한다.
2. `GlobalExceptionHandler`가 `ValidationErrorStatusResolver`에 예외와 요청 URI를 전달한다.
3. resolver가 field/global error의 `defaultMessage`를 기준으로 세부 `ErrorStatus`를 찾는다.
4. 매핑된 status가 있으면 해당 코드와 메시지로 실패 응답을 만든다.
5. 매핑이 없으면 기존 URI 기반 대표 validation status로 fallback한다.

## 5. Swagger 반영

- 온보딩 개인 정보 입력은 대표 세부 오류 예시를 추가한다.
- 기록 생성은 이미지 키 오류, 식사 payload 오류, 운동 payload 오류 예시를 추가한다.
- 댓글 생성은 댓글 내용 오류 예시를 추가한다.

## 6. 인수조건

- [x] 응답 구조가 변경되지 않는다.
- [x] 운동 일정 3개 미만 요청은 `MEMBER309`로 응답한다.
- [x] 식사 설정 조합 오류는 `MEMBER307` 또는 `MEMBER308`로 응답한다.
- [x] 식사 기록 payload 오류는 `RECORD306`으로 응답한다.
- [x] 운동 기록 payload 오류는 `RECORD307`로 응답한다.
- [x] 댓글 내용 오류는 `RECORD309`로 응답한다.
- [x] 기존 대표 validation code fallback은 유지된다.

## 7. 영향 범위

- `ErrorStatus`
- `GlobalExceptionHandler`
- `ValidationErrorStatusResolver`
- 온보딩/기록 Swagger RestDocs 테스트
