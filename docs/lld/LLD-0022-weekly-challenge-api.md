# LLD-0022: 주간 챌린지 API

> Low-Level Design. 이 문서는 #50 구현과 PR 본문의 오라클이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #50 |
| 관련 ADR | ADR-0003, ADR-0004, ADR-0007 |
| 작성자 | Codex |
| 작성일 | 2026-07-05 |

## 1. 목적 / 배경

주간 챌린지는 전역 원본을 한 번 운영하고, 각 그룹이 연결된 별도 진행 항목에서 사진 인증을 수행하는 챌린지다.
앱은 이번 주 챌린지 목록, 챌린지 상세, 그룹원 인증 이미지, 내 인증 업로드 결과를 조회할 수 있어야 한다.

## 2. 범위

### In scope

- 이번 주 진행 중인 주간 챌린지 목록 조회.
- 주간 챌린지 상세 조회.
- 그룹원 인증 이미지 조회.
- 주간 챌린지 인증 imageKey와 이미지 crop region 저장.
- 운영자 전역 주간 사진 챌린지 등록·조회·수정·삭제.
- 그룹 구성원 권한 검증.
- 중복 인증 방지.
- Swagger 성공/예외 응답 문서화.

### Out of scope

- 완료된 전역 주간 챌린지의 재사용 정책.
- 챌린지 완료 자동 판정.
- 공유용 합성 이미지 생성.
- 주간 챌린지 완료 알림 발송.

## 3. 데이터 모델

- `challenge`: 사진 챌린지 제목과 설명 메타데이터. `challengeType = PHOTO`.
- `global_weekly_challenge`: 전역 운영 원본. 원본 `challenge_id`, 멱등성 키, 시작일, 마감일을 소유한다.
- `group_challenge`: 그룹에서 특정 기간 동안 진행하는 챌린지 인스턴스. 전역 항목은 `global_weekly_challenge_id`로 원본을 연결하고, 기존 그룹별 항목은 이 값이 `null`인 레거시 데이터로 유지한다.
- `challenge_proof`: 그룹 챌린지에 대한 회원별 사진 인증 기록.
  - `image_key`: 원본 이미지 key.
  - `crop_x`, `crop_y`, `crop_width`, `crop_height`: 원본 이미지 기준 관심 영역 정규화 좌표. 없으면 null.

운영자는 전역 관리자 API로 원본을 한 번 생성한다. 생성 시 모든 활성 그룹에 기존 동작과 동일한 별도 `challenge`와 `group_challenge`를 만들고, 각 진행 인스턴스만 원본을 참조한다. 따라서 그룹별 `challengeId`의 기존 고유성 및 모바일 응답 계약을 유지한다. 새 그룹 생성 시 진행 중인 원본도 같은 방식으로 자동 연결한다.
관리자 API는 `X-Admin-Api-Key` 헤더가 `ADMIN_API_KEY` 환경변수와 일치해야 호출할 수 있다.

## 4. API 동작

### 이번 주 주간 챌린지 조회

```http
GET /api/v1/groups/{groupId}/challenges/weekly
```

1. 요청 회원이 그룹에 참여 중인지 확인한다.
2. 오늘 날짜가 `startsOn <= today <= endsOn` 범위에 포함되는 `IN_PROGRESS` 상태 PHOTO 그룹 챌린지를 조회한다.
3. 각 그룹 챌린지의 인증 수를 참여 인원으로 계산한다.
4. 카드 표시를 위해 시작일, 마감일, 남은 일수와 먼저 인증한 최대 3명의 회원 프로필을 반환한다.
5. 진행 중인 주간 챌린지가 없으면 빈 배열을 반환한다.

### 주간 챌린지 상세 조회

```http
GET /api/v1/weekly-challenges/{challengeId}
```

1. 요청 회원이 활성 회원인지 확인한다.
2. `challengeId`가 PHOTO 타입 챌린지인지 확인한다.
3. 챌린지명과 상세 설명을 반환한다.

### 그룹원 인증 이미지 조회

```http
GET /api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs
```

1. 요청 회원이 그룹에 참여 중인지 확인한다.
2. `groupChallengeId`가 요청 그룹의 PHOTO 그룹 챌린지인지 확인한다.
3. 인증 이미지를 업로드 순서대로 조회한다.
4. 각 인증에 대해 이미지 URL, 이미지 관심 영역 좌표, 회원 id, 그룹 내 닉네임, 프로필 이미지 URL을 반환한다.

### 주간 챌린지 인증 업로드

```http
POST /api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs
```

요청:

```json
{
  "imageKey": "weekly-challenges/1/2026/07/4111584723969.jpg",
  "imageCropRegion": {
    "x": 0.22985781990521326,
    "y": 0.3815165876777251,
    "width": 0.5402843601895736,
    "height": 0.23696682464454974
  }
}
```

1. 요청 회원이 그룹에 참여 중인지 확인한다.
2. `groupChallengeId`가 요청 그룹의 PHOTO 그룹 챌린지인지 확인한다.
3. 같은 회원이 같은 그룹 챌린지에 이미 인증했다면 `CHALLENGE304`를 반환한다.
4. `imageCropRegion`이 있으면 `x/y/width/height`가 0~1 정규화 좌표이고 원본 범위를 넘지 않는지 검증한다.
5. 인증 기록을 저장하고 인증 id, 그룹 챌린지 id, 이미지 URL, 이미지 관심 영역 좌표를 반환한다.

인증은 `IN_PROGRESS` 상태이며 시작일과 마감일 사이에 있는 챌린지에서만 가능하다.

### 운영자 전역 주간 사진 챌린지 관리

```http
POST /api/v1/admin/weekly-challenges
X-Admin-Api-Key: {ADMIN_API_KEY}
```

```json
{
  "title": "엘리베이터 안 타고 올라가기",
  "description": "계단으로 이동한 사진을 인증해주세요.",
  "startsOn": "2026-08-03",
  "endsOn": "2026-08-09"
}
```

전역 원본과 사진 챌린지 메타데이터를 생성하고, 모든 활성 그룹에 원본을 참조하는 별도 사진 챌린지와 진행 인스턴스를 생성한다. 봇은 같은 승인 요청을 재시도할 때 동일한 `Idempotency-Key` 헤더를 전송해야 한다. 이미 처리된 키이면 새 데이터를 만들지 않고 기존 결과를 반환한다.

```http
GET /api/v1/admin/weekly-challenges
PUT /api/v1/admin/weekly-challenges/{globalWeeklyChallengeId}
DELETE /api/v1/admin/weekly-challenges/{globalWeeklyChallengeId}
```

수정은 원본과 연결된 그룹별 사진 챌린지 메타데이터 및 진행 인스턴스의 기간에 반영한다. 삭제는 원본과 연결된 사진 챌린지 메타데이터를 비활성화하며, 연결된 그룹 인증 데이터는 보존한다. 기존 그룹별 관리자 API는 레거시 그룹별 항목과의 호환성을 위해 유지한다.

## 5. 예외 코드

- `AUTH401`~`AUTH405`: 인증 실패.
- `MEMBER302`: 회원 없음.
- `GROUP302`: 그룹 없음.
- `GROUP306`: 그룹 참여 정보 없음.
- `CHALLENGE301`: 챌린지 요청값 검증 실패.
- `CHALLENGE302`: 챌린지를 찾을 수 없음.
- `CHALLENGE304`: 이미 주간 챌린지 인증을 완료함.

## 6. 테스트 시나리오

- 이번 주 진행 중인 PHOTO 그룹 챌린지와 참여 현황을 조회한다.
- PHOTO 챌린지 상세 정보를 조회한다.
- 그룹원 인증 이미지와 회원 표시 정보를 조회한다.
- 주간 챌린지 인증 imageKey와 imageCropRegion을 저장한다.
- 주간 챌린지 인증 이미지 응답에는 imageCropRegion을 포함하고, 값이 없으면 null로 반환한다.
- 이미 인증한 회원은 같은 그룹 챌린지에 중복 인증할 수 없다.
- Swagger에 성공/예외 응답이 생성된다.

## 7. 미결정 사항 (Open Questions)

- 완료 기준과 완료 알림 연결 방식 결정 필요.
- 공유용 합성 이미지 생성은 별도 이슈에서 설계 필요.
