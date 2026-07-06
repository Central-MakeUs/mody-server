# LLD-0025: 주간 챌린지 완료 공유 이미지 API

> Low-Level Design. 이 문서는 #77 구현과 PR 본문의 오라클이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #77 |
| 관련 ADR | ADR-0010, ADR-0012 |
| 작성자 | Codex |
| 작성일 | 2026-07-06 |

## 1. 목적 / 배경

주간 챌린지 완료 후 클라이언트는 그룹원 인증 이미지를 인원수에 맞게 그리드화한 공유 이미지를 보여줘야 한다.
현재 API는 고정 응답을 반환하므로, 완료된 그룹 챌린지를 검증하고 실제 인증 이미지 기반의 공유 이미지 정보를 반환해야 한다.

## 2. 범위

### In scope

- 완료된 PHOTO 타입 그룹 챌린지 검증.
- 주간 챌린지 인증 이미지 조회.
- 인증 이미지 개수 기반 rows/columns 계산.
- 공유 이미지 key와 URL 생성.
- Swagger 성공/예외 응답 문서화.
- 공유 이미지 저장소 추상화 설계.

### Out of scope

- 클라이언트 공유 시트 호출.
- 오래된 공유 이미지 배치 삭제.
- 디자인 템플릿 고도화.

## 3. API 동작

```http
POST /api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/share
```

1. access token의 회원이 활성 회원인지 확인한다.
2. 요청 회원이 그룹에 참여 중인지 확인한다.
3. `groupChallengeId`가 요청 그룹의 PHOTO 타입 그룹 챌린지인지 확인한다.
4. 그룹 챌린지가 `COMPLETED` 상태가 아니면 `CHALLENGE306`을 반환한다.
5. 인증 이미지가 없으면 `CHALLENGE307`을 반환한다.
6. 인증 이미지 수를 기준으로 rows/columns를 계산한다.
7. 공유 이미지 key를 `weekly-challenge-shares/{groupId}/{groupChallengeId}.jpg` 형식으로 생성한다.
8. 공유 이미지가 이미 저장되어 있으면 재생성하지 않고 기존 URL을 반환한다.
9. 저장되어 있지 않으면 인증 이미지를 읽어 그리드 이미지로 합성하고 저장한 뒤 URL을 반환한다.

## 4. 그리드 계산

- `columns = ceil(sqrt(count))`
- `rows = ceil(count / columns)`
- 예시:
  - 1명: 1 x 1
  - 2명: 1 x 2
  - 3~4명: 2 x 2
  - 5~6명: 2 x 3
  - 7~9명: 3 x 3
  - 10~12명: 3 x 4

## 5. 저장소 추상화

서버가 이미지를 합성하려면 기존 인증 이미지를 읽고 새 이미지를 저장해야 하므로, presigned URL 발급과 별도의 저장소 포트를 둔다.

- `ObjectStorage.exists(key)`: 공유 이미지 재사용 여부 확인.
- `ObjectStorage.read(key)`: 인증 이미지 바이트 조회.
- `ObjectStorage.write(key, bytes, contentType)`: 공유 이미지 저장.
- `ObjectStorage.toUrl(key)`: 클라이언트 접근 URL 생성.

GCS 구현은 `Storage` 클라이언트를 사용한다. 로컬 구현은 테스트와 로컬 실행을 위해 메모리 기반으로 둔다.

## 6. 예외 코드

- `AUTH401`~`AUTH405`: 인증 실패.
- `MEMBER302`: 회원 없음.
- `GROUP302`: 그룹 없음.
- `GROUP306`: 그룹 참여 정보 없음.
- `CHALLENGE302`: 챌린지를 찾을 수 없음.
- `CHALLENGE306`: 완료되지 않은 챌린지임.
- `CHALLENGE307`: 공유할 챌린지 인증 이미지가 없음.
- `UPLOAD303`: 저장소 설정이 올바르지 않음.
- `UPLOAD305`: 이미지 읽기 또는 저장 실패.

## 7. 테스트 시나리오

- 완료된 주간 챌린지의 인증 이미지로 공유 이미지 URL과 그리드 크기를 반환한다.
- 이미 공유 이미지가 있으면 재생성하지 않는다.
- 미완료 챌린지는 공유 이미지를 생성할 수 없다.
- 인증 이미지가 없으면 공유 이미지를 생성할 수 없다.
- Swagger에 성공/예외 응답이 생성된다.

## 8. 미결정 사항 (Open Questions)

- 공유 이미지 템플릿의 상세 디자인은 추후 확정한다. 현재는 정사각형 썸네일 그리드로 생성한다.
- 공유 이미지 삭제는 고아 이미지 배치 정책에서 함께 처리한다.
