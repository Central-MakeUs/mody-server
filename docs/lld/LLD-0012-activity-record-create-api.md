# LLD-0012: 기록 업로드 API

> Low-Level Design. 이 문서는 기록 업로드 API 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #40 |
| 관련 ADR | ADR-0003, ADR-0010 |
| 작성자 | Codex |
| 작성일 | 2026-07-01 |

## 1. 목적 / 배경

클라이언트가 Presigned URL로 이미지 파일을 직접 업로드한 뒤,
서버에는 업로드 결과인 `imageKey`와 식사/운동 기록 메타데이터를 저장한다.

## 2. 범위

### In scope

- 식사 기록 생성.
- 운동 기록 생성.
- 그룹 기록 생성 시 그룹 존재 및 참여 상태 검증.
- 기록 생성 API의 성공/예외 Swagger 명세.

### Out of scope

- 기록 목록 조회, 상세 조회, 댓글 작성 실제 구현.
- 기록 수정/삭제.
- 스토리지 객체 존재 여부 확인.

## 3. 인터페이스 / API

모든 구현 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```http
POST /api/v1/records
```

식사 기록 요청:

```json
{
  "groupId": 1,
  "recordType": "MEAL",
  "imageKey": "records/1/2026/07/4111584723968.jpg",
  "mealTime": "12:30",
  "menu": "샐러드",
  "exerciseDurationHours": null,
  "exerciseDurationMinutes": null,
  "exerciseName": null
}
```

운동 기록 요청:

```json
{
  "groupId": 1,
  "recordType": "EXERCISE",
  "imageKey": "records/1/2026/07/4111584723969.jpg",
  "mealTime": null,
  "menu": null,
  "exerciseDurationHours": 0,
  "exerciseDurationMinutes": 40,
  "exerciseName": "러닝"
}
```

## 4. 데이터 모델

- `activity_record`
  - `member_id`: 토큰에서 추출한 작성자 id.
  - `group_id`: 그룹 기록이면 저장, 개인 기록이면 null.
  - `record_type`: `MEAL`, `EXERCISE`.
  - `meal_time`, `menu`: 식사 기록 전용 필드.
  - `exercise_duration_minutes`, `exercise_name`: 운동 기록 전용 필드.
    API에서는 시간/분을 분리해 받고 서버는 총 분으로 환산해 저장한다.
  - `image_key`: Presigned URL 발급 결과로 받은 스토리지 객체 키.
  - `uploaded_at`: 서버 저장 시각.

외래키 제약조건은 사용하지 않고 id 값으로만 참조한다.

## 5. 처리 흐름

1. `@CurrentMember`가 access token에서 회원 id를 추출한다.
2. 활성 회원을 조회한다.
3. `groupId`가 있으면 활성 그룹인지 확인한다.
4. 그룹 기록이면 해당 회원이 `JOINED` 상태인지 확인한다.
5. `recordType`에 따라 식사 또는 운동 필드 조합을 검증한다.
6. Snowflake id를 발급하고 `activity_record`를 저장한다.
7. 생성된 기록 id를 반환한다.

## 6. 예외 / 에러 처리

- 인증 실패: `AUTH401`~`AUTH405`.
- 요청 검증 실패: `RECORD301`.
- 회원 없음: `MEMBER302`.
- 그룹 없음: `GROUP302`.
- 그룹 참여 정보 없음: `GROUP306`.

## 7. 인수조건 (Acceptance Criteria)

- [x] 인증 회원이 식사 기록을 생성할 수 있다.
- [x] 인증 회원이 운동 기록을 생성할 수 있다.
- [x] 그룹 기록은 참여 중인 그룹에만 생성할 수 있다.
- [x] records 도메인으로 발급된 이미지 키만 입력으로 허용한다.
- [x] Swagger에 성공/주요 예외 응답이 반영된다.
- [x] 서비스 단위 테스트가 주요 분기를 검증한다.

## 8. 영향 범위 / 마이그레이션

- `record` 패키지에 service와 repository가 추가된다.
- 기존 기록 입력 stub은 DB 저장 기반 구현으로 교체된다.
- `activity_record`는 기존 엔티티 구조에 필드 저장 로직만 추가된다.
- 별도 마이그레이션 파일은 없다.

## 9. 미결정 사항 (Open Questions)

- 기록 수정/삭제 정책은 후속 이슈에서 결정한다.
- 스토리지 업로드는 성공했지만 기록 생성이 실패한 고아 이미지는 배치로 정리한다.

## 10. 참고

- `docs/adr/ADR-0010-direct-image-upload-via-signed-url.md`
- `docs/erd/ERD-0001-initial-domain-model.md`
