# Swagger API Spec Guide

## 목적

로직 구현 전에 API 계약을 먼저 확인할 수 있도록 Swagger 명세 작성 기준을 통일한다.

## 생성 방식

- API 명세는 Spring REST Docs 테스트로 생성한다.
- 테스트는 `build/generated-snippets`에 스니펫을 만들고, `./gradlew openapi3`가 `build/api-spec/openapi3.json`을 생성한다.
- 컨트롤러에는 Swagger annotation을 붙이지 않는다.

## 공통 응답

모든 API는 `ApiResponse<T>` 형식을 사용한다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "OK",
  "result": {}
}
```

실패 응답도 같은 envelope을 사용한다.

```json
{
  "isSuccess": false,
  "code": "COMMON4003",
  "message": "입력값이 올바르지 않습니다.",
  "result": {
    "nickname": "닉네임은 필수입니다."
  }
}
```

## 인증

- 인증이 필요한 API는 JWT access token을 `Authorization: Bearer {token}` 형식으로 받는다.
- 로그인, 토큰 재발급, 헬스 체크처럼 공개 API는 문서화 테스트에서 인증 헤더 없이 작성한다.

## 문서화 테스트 규칙

- 컨트롤러에는 문서용 annotation을 붙이지 않는다.
- 문서화 테스트명은 `<Domain>ControllerDocsTest` 형식을 사용한다.
- 각 API는 `MockMvcRestDocumentationWrapper.document(...)`와 `resource(...)`로 summary, description, tag를 작성한다.
- 요청/응답 DTO는 record를 기본으로 하고, 필드 설명은 REST Docs field descriptor에 작성한다.
- 실제 로직이 없는 명세용 API는 `ApiResponse.ok(...)` 형태의 예시 응답만 반환한다.
- 이미지 업로드 API는 `PresignedUrlResponse`를 사용하고, DB에는 URL이 아니라 `imageKey`를 저장한다.

## 제외 범위

Swagger 명세 작업에서는 OAuth 검증, DB 저장, S3 업로드, 알림 발송, 걸음수 집계 같은 실제 비즈니스 로직을 구현하지 않는다.
