# LLD-0001: 헬스 체크 API + 공통 응답/예외 처리

> Low-Level Design. 이 문서는 해당 기능 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Approved |
| Issue | _(미발급 — GitHub Issue 생성 후 채움)_ |
| 관련 ADR | - |
| 작성자 | msk226 |
| 작성일 | 2026-06-23 |

## 1. 목적 / 배경
- 배포 후 서버 생존 여부를 확인할 헬스 체크 엔드포인트가 필요하다 (CI/배포 스크립트 health check 용도).
- 동시에 이후 모든 API가 사용할 **공통 응답 포맷**과 **전역 예외 처리** 기반을 마련한다. spot-kotlin의 `common/api` 코드를 가져와 mody에 맞게 포팅한다.

## 2. 범위
### In scope
- 공통 응답: `ApiResponse<T>`, `BaseCode`, `ReasonDto`, `SuccessStatus`.
- 공통 에러 코드: `ErrorStatus` — **공통 코드만**(INTERNAL_SERVER_ERROR, BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND).
- 전역 예외 처리: `GeneralException`, `GlobalExceptionHandler`.
- 헬스 체크 엔드포인트: `GET /health`.

### Out of scope
- 도메인별 에러 코드 (spot의 MEMBER/STUDY/POST 등은 가져오지 않음 — mody 도메인 정의 시 추가).
- DB/외부 의존성 상태 점검 (liveness만).
- 인증/인가.

## 3. 인터페이스 / API
`GET /health`

응답 (200):
```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "OK",
  "result": { "status": "UP" }
}
```

## 4. 데이터 모델
- 신규 엔티티 없음. `ApiResponse<T>`(직렬화 DTO)만 사용.

## 5. 처리 흐름
- `HealthController.health()` → `ApiResponse.ok(mapOf("status" to "UP"))` 반환. DB 조회 없음.

## 6. 예외 / 에러 처리
- `GlobalExceptionHandler`가 `GeneralException` → 해당 `ErrorStatus`, `MethodArgumentNotValidException` → 400 + 필드 에러, 그 외 `Exception` → 500으로 매핑하여 `ApiResponse.failure(...)` 반환.

## 7. 인수조건 (Acceptance Criteria)
- [ ] `GET /health` 가 200과 위 응답 본문을 반환한다.
- [ ] 공통 응답/예외 코드가 `cmc.mody.common.api` 패키지에 존재하고 컴파일된다.
- [ ] `GlobalExceptionHandler`가 등록되어 `GeneralException` 발생 시 매핑된 status로 응답한다.

## 8. 영향 범위 / 마이그레이션
- 신규 패키지 `cmc.mody.common.api`, `cmc.mody.health`. 기존 코드 영향 없음.

## 9. 미결정 사항 (Open Questions)
> ⚠️ 결정되지 않은 항목. PR 본문에서 빈칸으로 처리되며 확인 대상이 된다.
- [ ] 헬스 체크 경로를 `/health`로 확정할지 (배포 스크립트의 HEALTH_CHECK_PATH와 일치 필요).
- [ ] 헬스 체크에 DB 연결 상태를 포함할지 여부.
- [ ] GitHub Issue 번호 발급 및 연결.

## 10. 참고
- 출처: spot-kotlin `common/src/main/kotlin/kr/spot/common/api/**`.
