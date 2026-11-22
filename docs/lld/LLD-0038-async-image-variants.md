# LLD-0038: 기록 사진 축소본 비동기 생성

> Low-Level Design. 이 문서는 해당 기능 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Draft |
| Issue | _(미발급)_ |
| 관련 ADR | ADR-0012 (알림 아웃박스와 같은 작업 큐 방식) |
| 작성자 | Claude |
| 작성일 | 2026-11-22 |

## 1. 목적 / 배경

앱은 기록 목록·상세에서 사용자가 올린 원본 사진을 그대로 내려받는다.
2026-11-22 운영 기록 사진 최근 300장의 원본 크기는 중앙값 약 2.5MB, 상위 10% 약 3.8MB, 최대 약 6.9MB였다.
휴대폰 카메라 원본(예: 4032×3024)이라 목록 카드 크기에 비해 픽셀이 훨씬 많다.
LLD-0036에서 썸네일 생성·대역폭 최적화를 범위 밖으로 남겨 두었다.

업로드는 앱이 Presigned URL로 GCS에 직접 올리므로 서버는 업로드 시점에 바이트를 보지 않는다.
기록이 저장된 뒤 서버가 원본을 읽어 축소본을 만들고, 응답에 축소본 URL을 더한다.

## 2. 범위

### In scope

- 활동 기록·주간 챌린지 인증 사진의 축소본 생성: 목록용(긴 변 480px), 상세용(긴 변 1440px) JPEG.
- 기록 저장 커밋 뒤 작업 생성, 스케줄러 선점, 제한된 동시 처리, 재시도, 실패 상태 기록.
- 기존 사진을 조금씩 채우는 backfill (기본 꺼짐).
- 기록 목록·상세 응답에 `thumbnailImageUrl`, `displayImageUrl` 추가.

### Out of scope

- 앱의 축소본 사용 전환 (iOS·Android 작업 필요).
- 프로필 이미지 축소.
- WebP 등 ImageIO가 읽지 못하는 형식의 변환 (운영 3,699장 중 webp 8장, FAILED로 남고 원본 사용).
- 원본 삭제·재압축, CDN 도입.

## 3. 인터페이스 / API

`GET /api/v1/groups/{groupId}/records`, `GET /api/v1/groups/{groupId}/records/{recordId}` 응답의 각 기록에 필드를 더한다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `thumbnailImageUrl` | string \| null | 목록용 축소본 URL. 아직 없으면 null |
| `displayImageUrl` | string \| null | 상세용 축소본 URL. 아직 없으면 null |

`IMAGE_VARIANT_SERVE_IN_IMAGE_URL=true`이면 기존 `imageUrl` 자리에 축소본 URL을 넣는다(앱 수정 없이 적용).
목록은 `IMAGE_VARIANT_LIST_SIZE`(THUMBNAIL=480px, DISPLAY=1440px, 기본 DISPLAY), 상세는 1440px이다. 축소본이 없으면 원본이다.
같은 규칙을 주간 챌린지 인증 응답과 관리 화면(인증 피드, 그룹 기록, 신고, 회원 상세)의 기록 사진에도 적용한다. 앱·관리 화면에는 원본이 필요한 기능이 없다(2026-11-22 확인).
교체가 꺼져 있으면 `imageUrl`은 원본이고, 앱은 `thumbnailImageUrl ?? imageUrl`처럼 쓸 수 있다.
READY 축소본은 바뀌지 않으므로 서버 메모리에 최대 2만 건 캐시한다.
축소본은 자르지 않고 비율만 유지해 줄이므로 기존 `imageCropRegion` 비율 좌표를 그대로 적용할 수 있다.
EXIF 방향은 축소본 픽셀에 반영하고 축소본에는 EXIF를 남기지 않는다.

## 4. 데이터 모델

`image_variant` (V28). 원본 이미지 키당 한 행이며, 이 행이 작업 큐다.

| 컬럼 | 설명 |
| --- | --- |
| `image_key` (unique) | 원본 키 |
| `variant_status` | PENDING / PROCESSING / READY / FAILED |
| `attempt_count`, `next_attempt_at` | 재시도 |
| `picked_by`, `picked_at` | 선점한 서버와 시각 |
| `thumbnail_key`, `display_key` | `variants/w480/<원본 경로>.jpg`, `variants/w1440/<원본 경로>.jpg` |
| `original_bytes`, `thumbnail_bytes`, `display_bytes` | 효과 측정용 크기 |
| `processed_at`, `last_error` | 완료 시각, 마지막 오류(500자) |

기록 테이블에 컬럼을 더하지 않은 이유: 활동 기록과 챌린지 인증이 같은 처리를 쓰고, 같은 키를 다시 요청해도 한 번만 만들기 위해서다.

## 5. 처리 흐름

1. 기록·인증 저장 트랜잭션에서 `ImageVariantRequestedEvent`를 발행한다.
2. 커밋 뒤(`AFTER_COMMIT`) 새 트랜잭션에서 PENDING 행을 만든다. 실패해도 예외를 던지지 않는다.
   - 축소본이 없어도 앱은 원본으로 보여 줄 수 있으므로 기록 저장을 막지 않는다.
   - 알림 아웃박스(LLD-0019)는 `BEFORE_COMMIT`으로 함께 커밋했다. 알림은 누락이 곧 장애지만 축소본은 누락돼도 backfill로 채울 수 있어 결합도를 더 낮췄다.
3. 스케줄러(기본 5초)가 `FOR UPDATE SKIP LOCKED`로 최대 4건을 선점해 PROCESSING으로 바꾼다.
   - 블루그린 전환 중 두 컨테이너가 같은 작업을 가져가지 않는다.
   - PROCESSING으로 10분 넘게 남은 작업(처리 중 재시작)도 다시 선점한다.
4. 전용 실행기(스레드 2개, 대기열 8)에서 원본 읽기 → 디코딩 → 축소 → GCS 쓰기를 트랜잭션 밖에서 하고, 상태만 짧은 트랜잭션으로 바꾼다.
   - 디코딩 단계에서 최종 크기의 2배 이상 해상도는 건너뛰어(subsampling) 읽어 메모리와 시간을 줄인다.
   - 1440px 상세본을 먼저 만들고, 그것을 다시 줄여 480px 목록본을 만든다. 절반씩 줄이는 bilinear 보간을 쓴다.
5. 응답 조립 시 페이지의 이미지 키로 READY 행을 한 번에 조회해 URL을 붙인다(N+1 없음).
6. 신규 작업이 없을 때만 backfill이 생성 요청이 없는 기존 사진을 최신순으로 20장씩 요청한다.

## 6. 예외 / 에러 처리

| 상황 | 처리 |
| --- | --- |
| 저장소 읽기·쓰기 실패, 메모리 부족 | 재시도. `60초 × 시도 횟수` 뒤 다시 PENDING, 3회 실패 시 FAILED |
| ImageIO가 읽지 못하는 형식 | 재시도 없이 FAILED |
| 실행기 대기열이 가득 참 | 작업은 PROCESSING으로 남고 10분 뒤 다시 선점 |
| 작업 생성 실패 | 로그만 남김. 기록 저장은 이미 커밋됨. backfill이 채움 |

모든 경우 응답의 축소본 필드는 null이고 앱은 원본을 쓴다.

## 7. 인수조건 (Acceptance Criteria)

- [x] 4032×3024 원본에서 480×360, 1440×1080 JPEG를 만든다. 원본보다 키우지 않는다.
- [x] EXIF 방향을 적용하고, 투명 PNG는 흰 배경으로 만든다.
- [x] 읽을 수 없는 형식은 재시도 없이 FAILED, 저장소 오류는 재시도 후 FAILED.
- [x] 작업 생성 실패가 기록 저장에 영향을 주지 않는다.
- [x] 기록 목록·상세 응답에 축소본 URL을 더하고 REST Docs에 문서화한다.
- [x] 운영 사진 40장 로컬 측정: 실패 0장, 평균 원본 2,682KB → 목록용 29KB, 상세용 186KB, 장당 처리 중앙값 221ms(최대 795ms, 개발 노트북).
- [ ] dev 배포 후 신규 기록의 READY 전환과 GCS 객체 공개 접근 확인.
- [ ] 앱 전환 후 목록 화면 한 번 로딩의 이미지 전송량 비교.

## 8. 영향 범위 / 마이그레이션

- V28 `image_variant` 테이블 추가. 기존 테이블 변경 없음.
- 응답 필드 추가만 있어 기존 앱과 호환된다.
- 설정: `IMAGE_VARIANT_ENABLED`(기본 false), `IMAGE_VARIANT_BACKFILL_ENABLED`(기본 false) 등. 꺼 두어도 작업 행은 쌓이고, 켜면 그때부터 처리한다.
- GCS `variants/` 경로에 객체가 늘어난다. 운영 사진 3,699장 기준 약 0.8GB 추가 예상(평균 215KB × 장 수).
- `WeeklyChallengeShareImageGenerator`의 EXIF 처리를 `ExifOrientedImageReader`로 옮겨 함께 쓴다.

## 9. 미결정 사항 (Open Questions)

- [ ] 앱 전환 시점과 담당(iOS·Android).
- [ ] 축소본 객체에 `Cache-Control`을 지정할지(현재 버킷 기본값).

## 10. 참고

- LLD-0011 Presigned URL 발급 API, LLD-0019 알림 발송 아웃박스, LLD-0036 관리 화면 리뉴얼
