# LLD-0011: Presigned URL 발급 API

> Low-Level Design.
> 이 문서는 이미지 업로드용 Presigned URL 발급 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Draft |
| Issue | #38 |
| 관련 ADR | ADR-0004 |
| 작성자 | Codex |
| 작성일 | 2026-06-29 |

## 1. 목적 / 배경

식사/운동 기록과 인증 이미지 업로드를 위해 클라이언트가 직접 업로드할 URL이 필요하다.
서버는 업로드 가능한 URL과 이후 API에 전달할 image key를 발급한다.

## 2. 범위

### In scope

- 이미지 업로드용 Presigned URL 발급.
- 업로드 도메인별 object key 생성.
- 파일 확장자 검증.
- Swagger 성공/예외 응답 명세.

### Out of scope

- 실제 이미지 바이너리 업로드.
- 업로드 완료 검증 callback.
- 운영 스토리지 provider 확정.
- 고아 이미지 정리 배치 구현.

## 3. 인터페이스 / API

모든 구현 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
POST /api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg
```

응답:

```json
{
  "presignedUrl": "https://storage.example.com/records/1/2026/06/123.jpg?expiresIn=300",
  "imageKey": "records/1/2026/06/123.jpg",
  "expiresInSeconds": 300
}
```

## 4. 도메인 정책

지원 업로드 도메인:

- `record`: 식사/운동 기록 이미지.
- `profile`: 프로필 이미지.
- `weekly-challenge`: 주간 챌린지 인증 이미지.

지원 확장자:

- `jpg`, `jpeg`, `png`, `webp`.

## 5. 처리 흐름

1. `@CurrentMember`가 access token에서 회원 id를 추출한다.
2. `domain`을 지원 업로드 도메인으로 변환한다.
3. `fileName`에서 확장자를 추출하고 지원 확장자인지 검증한다.
4. `{domain-directory}/{memberId}/{yyyy/MM}/{id}.{ext}` 형식의 image key를 만든다.
5. `PresignedUrlIssuer`가 upload URL을 발급한다.
6. `presignedUrl`, `imageKey`, `expiresInSeconds`를 반환한다.

## 6. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- query parameter 누락: `COMMON4000`.
- 지원하지 않는 업로드 도메인: `UPLOAD301`.
- 지원하지 않는 파일 확장자: `UPLOAD302`.
- 스토리지 설정 누락: `UPLOAD303`.
- Signed URL 발급 실패: `UPLOAD304`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 인증 회원이 Presigned URL을 발급받을 수 있다.
- [x] image key는 도메인, 회원 id, 날짜, id를 포함한다.
- [x] 지원하지 않는 도메인과 확장자는 예외 응답으로 처리된다.
- [x] Swagger에 성공/예외 응답이 반영된다.
- [x] `./gradlew build`가 통과한다.

## 8. 영향 범위 / 마이그레이션

- `common` 패키지에 upload service와 issuer port가 추가된다.
- 실제 스토리지 provider 도입 시 `PresignedUrlIssuer` 구현체만 교체한다.
- Signed URL 발급 후 DB 참조 없이 남은 고아 이미지는 ADR-0004에 따라 배치로 정리한다.

## 9. 미결정 사항 (Open Questions)

- 없음.
