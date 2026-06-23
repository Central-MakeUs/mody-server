# LLD-0003: 공통 응답/예외 및 엔티티 기반

> Low-Level Design. 이 문서는 해당 기능 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Approved |
| Issue | _(미발급 — GitHub Issue 생성 후 채움)_ |
| 관련 ADR | ADR-0001, ADR-0002 |
| 작성자 | msk226 |
| 작성일 | 2026-06-23 |

## 1. 목적 / 배경
- 모든 API가 같은 응답 포맷과 에러 코드 체계를 사용하도록 공통 계약을 고정한다.
- 이후 도메인 엔티티가 동일한 감사 필드와 상태값을 갖도록 JPA Auditing 기반을 마련한다.
- 컨트롤러 입력 검증 실패를 공통 실패 응답으로 변환해 클라이언트 처리 비용을 줄인다.

## 2. 범위
### In scope
- 공통 응답 DTO: `ApiResponse<T>`.
- 상태 코드 인터페이스: `BaseCode`, `ReasonDto`.
- 성공/실패 상태 enum: `SuccessStatus`, `ErrorStatus`.
- 전역 예외 처리: `GlobalExceptionHandler`.
- 비즈니스 예외: `GeneralException`.
- Validation 예외 매핑: `MethodArgumentNotValidException`.
- 공통 엔티티 기반: `BaseEntity`, `Status`.
- JPA Auditing 활성화: `JpaConfig`.

### Out of scope
- 도메인별 상세 에러 코드 전체 정의.
- Spring Security 인증/인가 필터.
- DB 마이그레이션 도구 도입.
- soft delete를 자동 쿼리 필터로 강제하는 정책.

## 3. 인터페이스 / API
모든 API 응답은 다음 JSON 구조를 따른다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "OK",
  "result": {}
}
```

성공 응답은 `ApiResponse.ok(...)`, `ApiResponse.created(...)`, `ApiResponse.success(...)`를 사용한다.
실패 응답은 `ApiResponse.failure(...)`를 사용하며, 컨트롤러에서 직접 만들기보다 예외를 던져
`GlobalExceptionHandler`가 변환하도록 한다.

## 4. 데이터 모델
### ApiResponse
- `isSuccess`: 성공 여부.
- `code`: 클라이언트가 분기할 수 있는 문자열 코드.
- `message`: 사람이 읽을 수 있는 메시지.
- `result`: 응답 본문. null이면 JSON에서 제외된다.

### BaseEntity
- `createdAt`: `@CreatedDate`로 생성 시각 자동 기록.
- `updatedAt`: `@LastModifiedDate`로 수정 시각 자동 기록.
- `status`: `Status.ACTIVE` 기본값.
- `delete()`: `status`를 `INACTIVE`로 바꾸는 soft delete용 메서드.

## 5. 처리 흐름
- 정상 처리
  1. 컨트롤러 또는 서비스가 결과 DTO를 만든다.
  2. 컨트롤러는 `ApiResponse.ok(result)` 또는 `created(result)`를 반환한다.
- 비즈니스 예외
  1. 서비스가 `GeneralException(ErrorStatus.XXX)`를 던진다.
  2. `GlobalExceptionHandler`가 `status.httpStatus`와 `ApiResponse.failure(status)`로 응답한다.
- Validation 예외
  1. `@Valid` 요청 검증 실패 시 `MethodArgumentNotValidException`이 발생한다.
  2. field error를 `Map<String, String>`으로 모은다.
  3. `VALIDATION_FAILED` 코드와 함께 실패 응답으로 반환한다.
- 엔티티 감사 필드
  1. `JpaConfig`의 `@EnableJpaAuditing`이 Auditing을 활성화한다.
  2. `BaseEntity`를 상속한 엔티티가 저장/수정될 때 감사 필드가 자동 채워진다.

## 6. 예외 / 에러 처리
- `GeneralException` → 해당 `ErrorStatus.httpStatus`.
- `MethodArgumentNotValidException` → 400, `COMMON4003`, field error map.
- `IllegalArgumentException` → 400, `COMMON4000`, 예외 메시지.
- 그 외 `Exception` → 500, `COMMON500`.
- JWT 관련 예외 코드는 `JwtTokenProvider`가 `GeneralException`으로 변환한다.

## 7. 인수조건 (Acceptance Criteria)
- [x] 성공 응답 JSON은 `isSuccess`, `code`, `message`, `result` 순서를 유지한다.
- [x] null `result`는 JSON 응답에서 제외된다.
- [x] `GeneralException`은 지정된 `ErrorStatus`의 HTTP status로 변환된다.
- [x] Validation 실패는 field error map을 포함한 공통 실패 응답으로 변환된다.
- [x] `BaseEntity`는 `createdAt`, `updatedAt`, `status`를 제공한다.
- [x] JPA Auditing이 애플리케이션 설정에 등록되어 있다.

## 8. 영향 범위 / 마이그레이션
- 신규 API는 공통 응답 포맷을 따라야 한다.
- 신규 엔티티는 특별한 이유가 없으면 `BaseEntity`를 상속한다.
- `status = INACTIVE` 데이터의 조회 제외 정책은 아직 자동화되지 않았다.
- 로컬 실행은 현재 `local` 프로파일의 MySQL 연결이 필요하다.

## 9. 미결정 사항 (Open Questions)
> ⚠️ 결정되지 않은 항목. PR 본문에서 빈칸으로 처리되며 확인 대상이 된다. 추측으로 채우지 말 것.
- [ ] 도메인별 에러 코드 네이밍 규칙 최종안.
- [ ] soft delete 조회 제외를 `@SQLRestriction`으로 강제할지, repository/service 조건으로 처리할지.
- [ ] `Status` 값을 모든 엔티티에 공통 적용할지, 필요한 엔티티에만 둘지.
- [ ] `createdBy`, `updatedBy` 같은 auditor 필드 도입 여부.
- [ ] 로컬 개발에서 MySQL 없이 `/health`를 띄울 별도 profile을 둘지.

## 10. 참고
- 출처: `spot-kotlin/common/src/main/kotlin/kr/spot/common/api/**`.
- 출처: `spot-kotlin/common/src/main/kotlin/kr/spot/common/domain/BaseEntity.kt`.
