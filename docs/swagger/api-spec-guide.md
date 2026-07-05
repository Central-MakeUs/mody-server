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
- Swagger UI에서는 상단 `Authorize` 버튼에 access token을 한 번 등록해 전체 인증 API에 적용한다.
- 개별 API request field에는 `Authorization` 헤더를 반복해서 문서화하지 않는다.
- 로그인, 토큰 재발급, 헬스 체크처럼 공개 API는 문서화 테스트에서 인증 헤더 없이 작성한다.

## 목록 조회와 커서 페이징

- 최초 조회는 `cursor`를 생략한다.
- 다음 페이지가 있으면 응답의 `nextCursor`를 다음 요청의 `cursor`로 전달한다.
- 마지막 페이지는 `nextCursor: null`, `hasNext: false`로 표현한다.
- 커서 기준, 정렬 방향, `size` 기본값이 API마다 다르면 description에 명시한다.

## 이미지 업로드 흐름

- 클라이언트는 먼저 `POST /api/v1/uploads/presigned-url`로 `presignedUrl`과 `imageKey`를 발급받는다.
- 이미지 바이너리는 `presignedUrl`로 스토리지에 직접 업로드한다.
- 기록/프로필/챌린지 API에는 URL이 아니라 `imageKey`를 전달한다.
- 서버 응답에서 이미지를 보여줘야 하는 API는 접근 가능한 `imageUrl`을 응답한다.

## 문서화 테스트 규칙

- 컨트롤러에는 문서용 annotation을 붙이지 않는다.
- 문서화 테스트명은 `<Domain>ControllerDocsTest` 형식을 사용한다.
- 각 API는 `MockMvcRestDocumentationWrapper.document(...)`와 `resource(...)`로 summary, description, tag를 작성한다.
- description에는 화면 분기, 입력 책임, 페이징 기준, 이미지 업로드 선행 조건처럼 클라이언트 구현에 필요한 정책을 포함한다.
- 성공 응답뿐 아니라 인증 실패, 검증 실패, 리소스 없음 등 클라이언트가 분기해야 하는 주요 예외 응답도 테스트로 생성한다.
- 요청/응답 DTO는 record를 기본으로 하고, 필드 설명은 REST Docs field descriptor에 작성한다.
- 실제 로직이 없는 명세용 API는 `ApiResponse.ok(...)` 형태의 예시 응답만 반환한다.
- 미구현 API는 summary 앞에 `[미구현]`을 붙이고, description에 실제 비즈니스 로직이 연결되지 않았음을 명시한다.
- 이미지 업로드 API는 `PresignedUrlResponse`를 사용하고, DB에는 URL이 아니라 `imageKey`를 저장한다.

## 도메인별 명세 기준

- Auth/OAuth: 네이티브 앱용 provider token 로그인과 서버 callback 로그인을 구분한다.
- Onboarding: 개인 정보 입력 완료, 그룹 온보딩 완료, 메인 진입 가능 여부를 응답 필드 설명에 명확히 남긴다.
- Group: 참여 가능 그룹 수, 그룹 최대 인원, 그룹 탈퇴 후 접근 제한을 description에 포함한다.
- Feed: 기록 입력 시 `MEAL`/`EXERCISE`별 필수/null 필드와 상세 캐러셀 페이징 기준을 명시한다.
- Mypage: 알림 수신 여부와 식사/운동 시간표 입력 책임을 구분한다.
- Notification: 목록 조회는 커서 페이징으로 문서화하고, 푸시 토큰은 `deviceId` 기준 갱신/해제를 설명한다.

## 제외 범위

Swagger 명세 작업에서는 OAuth 검증, DB 저장, S3 업로드, 알림 발송, 걸음수 집계 같은 실제 비즈니스 로직을 구현하지 않는다.
