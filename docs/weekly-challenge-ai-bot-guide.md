# 주간 챌린지 AI 봇 연동 가이드

## 목적

AI가 다음 주 주간 챌린지 후보를 만들고, Slack에서 운영자 승인을 받은 뒤 서버에 전역 주간 챌린지를 한 번 등록한다. 등록이 성공하면 서버가 모든 활성 그룹에 기존 모바일 API가 사용하는 그룹별 진행 데이터를 생성한다.

## 운영 흐름

```text
AI 후보 생성
  -> Slack 승인 요청
  -> 운영자 승인
  -> 전역 관리자 API 1회 호출
  -> 서버가 활성 그룹별 진행 항목 생성
  -> 봇이 Slack 스레드에 등록 결과 회신
```

Slack 승인 전에는 등록 API를 호출하지 않는다. 승인 메시지에 관리자 키를 포함하지 않는다.

## 인증

로그인 세션은 사용하지 않고 고정 API 키를 요청 헤더로 보낸다.

```http
X-Admin-Api-Key: {ADMIN_API_KEY}
```

서버의 키는 `ADMIN_API_KEY` 환경변수로 설정한다. 봇은 키를 소스 코드, Slack 메시지, 로그에 저장하지 말고 봇 실행 환경의 Secret Manager 또는 환경변수로 주입한다. Dev와 Prod 키는 서로 다른 값으로 관리한다.

## 등록 API

```http
POST {MODY_API_BASE_URL}/api/v1/admin/weekly-challenges
X-Admin-Api-Key: {ADMIN_API_KEY}
Idempotency-Key: weekly-2026-09-14
Content-Type: application/json
```

```json
{
  "title": "엘리베이터 안 타고 계단으로 이동하기",
  "description": "계단으로 이동한 사진을 인증해주세요.",
  "startsOn": "2026-09-14",
  "endsOn": "2026-09-20"
}
```

`startsOn`과 `endsOn`은 `yyyy-MM-dd` 형식의 날짜다. 제목은 50자 이하, 설명은 500자 이하이며 종료일은 시작일보다 빠를 수 없다.

응답의 `linkedGroupCount`는 등록 시점에 연결된 활성 그룹 수다. API 한 번으로 처리되므로 그룹별 API를 반복 호출하지 않는다.

같은 승인 요청을 재시도할 때는 반드시 같은 `Idempotency-Key`를 사용한다. 이미 처리된 키라면 새 챌린지나 그룹 진행 항목을 만들지 않고 기존 등록 결과를 반환한다.

## 조회·수정·삭제

```http
GET    {MODY_API_BASE_URL}/api/v1/admin/weekly-challenges
PUT    {MODY_API_BASE_URL}/api/v1/admin/weekly-challenges/{globalWeeklyChallengeId}
DELETE {MODY_API_BASE_URL}/api/v1/admin/weekly-challenges/{globalWeeklyChallengeId}
POST   {MODY_API_BASE_URL}/api/v1/admin/weekly-challenges/{globalWeeklyChallengeId}/sync-groups
```

수정 시 전역 원본과 연결된 그룹별 챌린지 제목, 설명, 기간이 함께 갱신된다. 삭제는 원본과 그룹별 챌린지를 비활성화하고 기존 인증 데이터는 보존한다. `sync-groups`는 자동 연결에 실패한 활성 그룹만 보정하며 이미 연결된 그룹은 건너뛴다.

## 기존 API와의 구분

기존 그룹별 관리자 API는 레거시 데이터 호환을 위해 유지한다.

```text
/api/v1/admin/groups/{groupId}/weekly-challenges
```

신규 주간 챌린지는 반드시 전역 API를 사용한다. 모바일 클라이언트의 주간 챌린지 조회, 인증, 완료, 콜라주, 알림 API는 변경하지 않는다.

## 어드민 페이지

`/admin/challenges` 접속 후 관리자 API 키로 인증하고, 좌측의 `전역 주간 챌린지` 메뉴에서 등록·조회·수정·삭제한다. 등록 시 연결 그룹 수를 확인할 수 있다.
