# LLD-0009: 마이페이지 회원 정보 및 체중 기록 API

> Low-Level Design. 이 문서는 마이페이지 1차 API 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Draft |
| Issue | #32 |
| 관련 ADR | ADR-0003 |
| 작성자 | Codex |
| 작성일 | 2026-06-28 |

## 1. 목적 / 배경

마이페이지 화면에서 현재 회원의 기본 정보와 체중 변화를 조회해야 한다.
체중 추가 시 이전 체중 기록 대비 증감 값을 서버에서 계산해 저장한다.

## 2. 범위

### In scope

- 내 정보 조회.
- 프로필 조회.
- 프로필 수정.
- 체중 기록 변화 조회.
- 체중 추가.

### Out of scope

- 회원 탈퇴.
- 로그아웃.
- 알림, 식사 시간, 운동 일정 수정.
- 그룹 구성원 조회와 그룹 나가기.

## 3. 인터페이스 / API

모든 구현 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
GET /api/v1/mypage/me
GET /api/v1/mypage/profile
PATCH /api/v1/mypage/profile
GET /api/v1/mypage/weights
POST /api/v1/mypage/weights
```

프로필 수정 요청:

```json
{
  "nickname": "민석",
  "birthDate": "2000-01-01"
}
```

체중 추가 요청:

```json
{
  "weightKg": 72.5
}
```

## 4. 데이터 모델

- `member`
  - `nickname`, `birth_date`, `profile_image_key`, `created_at`을 조회한다.
  - 프로필 수정은 `nickname`, `birth_date`만 변경한다.
- `social_account`
  - `member_id`로 첫 활성 소셜 계정을 조회해 `login_type`을 응답한다.
- `weight_record`
  - `member_id`, `recorded_on`, `weight_kg`, `change_from_previous_kg`를 저장한다.

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

1. `@CurrentMember`가 access token에서 회원 id를 추출한다.
2. 회원 조회는 활성 회원만 대상으로 한다.
3. 내 정보 조회는 회원 생성일 기준으로 함께한 날짜를 계산한다.
4. 프로필 조회는 회원과 첫 활성 소셜 계정의 로그인 타입을 반환한다.
5. 프로필 수정은 닉네임과 생년월일을 검증한 뒤 회원 엔티티에 반영한다.
6. 체중 기록 조회는 최신 기록일부터 내림차순으로 반환한다.
7. 체중 추가는 직전 체중과의 차이를 계산해 `change_from_previous_kg`에 저장한다.

## 6. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 요청 검증 실패: `MYPAGE301`.
- 회원 없음: `MEMBER302`.
- 소셜 계정 없음: `MYPAGE302`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 인증 회원이 내 정보를 조회할 수 있다.
- [x] 인증 회원이 프로필을 조회할 수 있다.
- [x] 인증 회원이 닉네임과 생년월일을 수정할 수 있다.
- [x] 인증 회원이 체중 기록 변화를 조회할 수 있다.
- [x] 인증 회원이 체중을 추가하면 이전 기록 대비 증감이 계산된다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.
- [x] `./gradlew build`가 통과한다.

## 8. 영향 범위 / 마이그레이션

- `mypage` 패키지에 service가 추가된다.
- `member` repository에 마이페이지 조회용 query method가 추가된다.
- 기존 마이페이지 API stub 중 1차 범위 API만 DB 기반 구현으로 교체된다.

## 9. 미결정 사항 (Open Questions)

- 없음.

## 10. 참고

- `docs/erd/ERD-0001-initial-domain-model.md`
