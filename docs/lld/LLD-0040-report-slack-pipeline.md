# LLD-0040: 신고 접수 Slack 알림과 Slack에서 숨김 · 기각

> Low-Level Design. 이 문서는 해당 기능 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Review (dev 실험) |
| Issue | #298 |
| 관련 ADR | - |
| 작성자 | Claude |
| 작성일 | 2026-11-25 |

## 1. 목적 / 배경

기록 신고(LLD-0037)와 댓글 신고(LLD-0039)는 관리 화면에서 처리할 수 있지만, 운영자가 화면을 직접 열기 전에는 신고가 들어온 줄 모른다.
신고가 들어오면 Slack으로 바로 알리고, 그 메시지에서 숨김·기각까지 끝낼 수 있게 해서 콘솔 · Slack 봇 · 작업 이력을 한 흐름으로 묶는다.

### 기준선 (2026-11-25 운영 DB, 읽기 전용 조회)

| 구분 | 총 신고 | 대상 수 | 대기 | 숨김 | 기각 | 첫 신고 | 최근 신고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 기록 신고 | 5 | 5 | 5 | 0 | 0 | 2026-07-31 | 2026-11-17 |
| 댓글 신고 | 1 | 1 | 1 | 0 | 0 | 2026-11-24 | 2026-11-24 |

- 월별: 기록 7월 1 · 8월 3 · 11월 1, 댓글 11월 1.
- 접수부터 처리까지 시간(`resolved_at - created_at`): **처리된 신고가 0건이라 중앙값·최대값을 낼 수 없다.** 가장 오래 대기한 신고는 117일째다.
- 관리자 계정은 ADMIN 1개뿐이다. OPERATOR·VIEWER 계정은 아직 없다.

## 2. 범위

### In scope

- 기록·댓글 신고가 **새로 저장되고 커밋된 뒤** Slack 웹훅으로 알린다. 같은 신고자의 재신고(기존 신고 반환)는 알리지 않는다.
- 같은 대상(기록 또는 댓글)의 알림을 묶음 시간 안에 한 번만 보낸다.
- 알림에 숨김·기각 버튼을 붙인다(설정으로 켠다). 버튼은 Slack 봇(`mody-slack-bot`, Socket Mode)이 받아 서버의 Slack 전용 처리 API를 부른다.
- Slack 사용자 ↔ 관리자 계정 연결(`admin_account.slack_user_id`). 처리자와 작업 이력에 실제로 누른 관리자가 남는다.
- 숨김·기각 이중 처리 방지(콘솔·Slack 동시 처리 포함).
- 콘솔 신고 화면에 `?kind=comments` 바로가기.

### Out of scope

- 신고 사유 수집. 앱 신고 API가 사유를 받지 않고 저장 컬럼도 없다(§9).
- 콘솔에서 처리했을 때 이미 올라간 Slack 메시지를 바로 고치는 것. 웹훅은 메시지 ID를 돌려주지 않는다. 버튼을 누르면 그때 "이미 처리됨"으로 바뀐다.
- 관리자 계정·역할·Slack 연결을 관리하는 화면. 역할과 마찬가지로 지금은 SQL로 넣는다.
- 운영 적용. dev에서 먼저 실험한다(§8).

## 3. 인터페이스 / API

### 3.1 신고 알림 (서버 → Slack 웹훅)

설정(`report-alert.slack.*`):

| 키 | 환경변수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `enabled` | `REPORT_ALERT_ENABLED` | `false` | 끄면 보내지 않는다 |
| `webhook-url` | `REPORT_ALERT_WEBHOOK_URL` | 빈 값 | 비어 있으면 보내지 않는다(로컬·테스트) |
| `console-base-url` | `REPORT_ALERT_CONSOLE_BASE_URL` | 빈 값 | 콘솔 링크 앞부분. 비어 있으면 링크를 뺀다 |
| `group-window` | `REPORT_ALERT_GROUP_WINDOW` | `10m` | 같은 대상 알림 묶음 시간 |
| `buttons-enabled` | `REPORT_ALERT_BUTTONS_ENABLED` | `false` | 숨김·기각 버튼을 붙인다 |

메시지(Block Kit, 웹훅 본문 `{text, blocks}`):

- 제목: `[dev] 기록 신고` / `[dev] 댓글 신고`
- 대상 ID(댓글이면 기록 ID 함께), 신고 사유(`앱에서 받지 않음`), 누적 신고 수, 대기 신고 수, 작성자 닉네임(탈퇴면 `탈퇴한 회원`), 콘솔 바로가기 `/admin/reports?kind=records|comments`
- 넣지 않는 것: 신고자 정보, 기록 제목·메뉴, 댓글 본문, 이미지, 그룹명
- 버튼: `report_hide`(위험 표시, 확인 창), `report_dismiss`. 값은 `RECORD:<id>` / `COMMENT:<id>`

### 3.2 Slack 처리 API (봇 → 서버)

| 메서드 | 경로 |
| --- | --- |
| POST | `/api/v1/admin/slack/reports/records/{recordId}/hide` · `/dismiss` |
| POST | `/api/v1/admin/slack/reports/comments/{commentId}/hide` · `/dismiss` |

요청 헤더:

| 헤더 | 값 |
| --- | --- |
| `X-Slack-User-Id` | 버튼을 누른 Slack 사용자 ID(봇이 Socket Mode payload에서 꺼낸 값) |
| `X-Slack-Action-Timestamp` | 유닉스 초 |
| `X-Slack-Action-Signature` | `v1=` + hex(HMAC-SHA256(secret, `v1:{timestamp}:{METHOD}:{path}:{slackUserId}`)) |

응답(`ApiResponse.result`):

```json
{
  "outcome": "RESOLVED",            // 또는 ALREADY_RESOLVED
  "kind": "RECORD",                 // 또는 COMMENT
  "targetId": "123",
  "resolution": "HIDDEN",           // 또는 DISMISSED
  "resolvedBy": "ops@example.com",
  "resolverSlackUserId": "U123",    // 처리자 계정에 연결된 Slack ID, 없으면 null
  "resolvedAt": "2026-11-25T10:00:00",
  "resolvedReports": 2              // 이번에 닫은 신고 수, ALREADY_RESOLVED면 0
}
```

| 상황 | 응답 |
| --- | --- |
| 서명 없음·틀림·5분 넘은 시각, 비밀값 미설정 | 401 `COMMON401` |
| 연결된 관리자 계정 없음, 비활성 계정, VIEWER | 403 `COMMON403` |
| 대상의 신고가 하나도 없음 | 404 |
| 대기 신고 없음(이미 처리됨) | 200 `ALREADY_RESOLVED` + 마지막 처리 결과 |

- 이 경로는 서명 인증만 받는다. `X-Admin-Api-Key`나 콘솔 세션으로는 부를 수 없다(처리자가 `machine`으로 남지 않게).
- 다른 관리자 경로는 서명 인증을 받지 않는다.
- 서버 설정: `admin.slack-action.secret`(`ADMIN_SLACK_ACTION_SECRET`). 비어 있으면 이 API는 항상 401이다.

## 4. 데이터 모델

`V29__add_admin_account_slack_user_id.sql`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `slack_user_id` | VARCHAR(32) NULL, UNIQUE | 이 관리자 계정으로 처리할 Slack 사용자 ID |

NULL 허용 컬럼 추가라 블루그린 배포 중 이전 버전에도 영향이 없다. 연결은 지금 역할 변경과 같은 방식(SQL)으로 한다.

## 5. 처리 흐름

### 5.1 알림

```text
신고 API 트랜잭션
  └ 새 신고 저장 → ReportFiledEvent 발행
커밋
  └ ReportAlertListener (AFTER_COMMIT, 요청 스레드)
       ├ 설정 꺼짐 → 끝
       ├ ReportAlertThrottle: 같은 대상이 묶음 시간 안에 이미 알렸으면 → 끝
       └ ReportAlertSender.send (@Async reportAlertExecutor)
            ├ 누적·대기 신고 수, 작성자 닉네임 조회(읽기 전용)
            ├ 메시지 구성 → 웹훅 POST
            └ 실패 → WARN 로그(종류·대상 ID·신고 ID)
```

- 리스너 전체를 예외로부터 감싼다. AFTER_COMMIT 리스너의 예외는 이미 커밋된 요청을 500으로 만들 수 있기 때문이다.
- 조회는 별도 스레드의 새 읽기 전용 트랜잭션에서 한다. 커밋 직후 같은 스레드의 연결에서 조회하지 않는다.

**아웃박스와 비교한 선택 — 커밋 후 이벤트 + 실패 로그**

| | 알림 아웃박스(LLD-0019 방식) | 커밋 후 이벤트 + 실패 로그 (선택) |
| --- | --- | --- |
| 전달 보장 | 재시도, 서버 재시작에도 남음 | 한 번 시도, 실패·재시작 중이면 유실 |
| 비용 | 테이블·스케줄러·상태 전이·보관 정리 | 클래스 몇 개, 마이그레이션 없음 |
| 유실의 결과 | - | 신고는 DB에 남고 콘솔 대기 목록에 보인다 |

알림은 신고 데이터의 사본을 알리는 신호일 뿐이고, 원본(`record_report`, `comment_report`)과 처리 화면은 그대로 남는다.
신고량도 4개월 6건이다. 유실을 0으로 만드는 비용보다, 유실돼도 콘솔에서 보인다는 점이 크다고 보고 커밋 후 이벤트를 택했다.
유실을 줄이는 보완은 §9의 "대기 신고 정기 요약"으로 한다.

**묶음 — 첫 신고는 바로, 이후는 묶음 시간 동안 생략**

- 묶음 단위는 `RECORD:<id>` / `COMMENT:<id>`다. 첫 신고는 바로 알리고, 묶음 시간 안의 같은 대상 신고는 보내지 않는다. 묶음 시간이 지나 들어온 신고는 다시 알리며 그때의 누적·대기 수를 싣는다.
- 기존 푸시 토큰 대량 비활성화 알림(`PushTokenInvalidationAlertService`)과 같은 메모리 방식이다. 서버 500 알림(LLD-0029)은 중복 억제가 없어 참고할 수 없었다.
- 대안인 "묶음 시간이 끝날 때 모아서 한 번"은 첫 알림이 늦어져 "바로 알린다"는 목표와 맞지 않아 택하지 않았다.
- 메모리 방식이라 재시작하면 묶음 기록이 사라진다. 그때 같은 대상 알림이 한 번 더 갈 수 있지만 손해가 작다.

### 5.2 Slack에서 처리

```text
Slack 버튼 클릭
  └ mody-slack-bot (Socket Mode, app.action)
       ├ ack
       ├ 환경·채널 확인 (운영은 지금 막음, §8)
       ├ 서명 헤더를 붙여 서버 Slack 처리 API 호출
       └ 결과로 원래 메시지 교체(response_url): "처리됨: 숨김 · <@처리자> · 시각"
                                         또는 "이미 처리됨: 기각 · 처리자 · 시각"
         401/403/오류 → 누른 사람에게만 보이는 안내, 메시지는 그대로
서버
  └ AdminRequestInterceptor
       ├ /api/v1/admin/slack/** → SlackActionAuthenticator: 서명·시각 확인 → slack_user_id로 계정 조회
       ├ 계정 없음·비활성·VIEWER → 403
       └ principal = 그 관리자 계정 → 컨트롤러 → 기존 hide/dismiss (resolved_by = 그 계정 이메일)
  └ afterCompletion → admin_audit_log (actor_admin_id = 그 계정, path에 /slack/)
```

**Slack 사용자와 관리자 계정 연결 — 선택: `admin_account.slack_user_id` + 봇 서명 위임**

| 방식 | 판단 |
| --- | --- |
| 봇이 `X-Admin-Api-Key`만 들고 호출 | 처리자·작업 이력이 `machine`으로 남고, 역할 검사가 없다. 요구 사항을 못 맞춰 기각 |
| 봇 설정에 Slack ID → 역할 표 | 콘솔 역할과 따로 관리돼 어긋난다. 기각 |
| Slack 프로필 이메일 ↔ 관리자 이메일 | 추가 컬럼이 없지만, Google 관리자 이메일과 Slack 이메일이 다르면 연결되지 않고 `users:read.email` 권한도 더 필요하다. 기각 |
| **`slack_user_id` 컬럼 + 봇 HMAC 서명** | 역할은 콘솔과 같은 `admin_account.role`을 쓰고, 처리자·작업 이력에 실제 계정이 남는다. 선택 |

- Slack 사용자 ID는 Slack이 Socket Mode로 봇에 준 값이라 봇은 믿을 수 있다. 서버는 "봇이 그렇게 말했다"를 공용 비밀값 서명으로 확인한다.
- 남는 위험: 비밀값을 가진 쪽은 연결된 어떤 관리자로도 처리할 수 있다. 그래서 일반 `ADMIN_API_KEY`와 다른 전용 비밀값을 쓰고, 이 경로 4개(숨김·기각)만 받으며, 봇은 에이전트에 넘기는 환경변수에서 비밀값을 뺀다(`SECRET` 이름 필터).
- 재전송: 서명에 시각을 넣고 5분이 지나면 거절한다. 5분 안의 재전송은 이미 처리된 결과만 돌려준다.

**이중 처리 방지**

- 숨김·기각은 대상의 대기 신고를 `PESSIMISTIC_WRITE`(`SELECT ... FOR UPDATE`)로 잠근 뒤 처리한다. 콘솔과 Slack에서 동시에 눌러도 늦은 쪽은 잠금이 풀린 뒤 대기 신고가 없다고 본다.
- 콘솔 API는 기존처럼 404, Slack API는 `ALREADY_RESOLVED`와 마지막 처리 결과를 돌려준다.
- 봇은 처리 후 원래 메시지의 버튼을 없애고 결과로 바꾼다.

## 6. 예외 / 에러 처리

- 웹훅 실패, 조회 실패: WARN 로그만. 신고 API 응답은 바뀌지 않는다.
- 서명 실패: 401. 계정 미연결·VIEWER: 403. 봇은 누른 사람에게만 이유를 보여준다.
- 봇이 서버에 닿지 못함: 누른 사람에게 "처리하지 못했다, 콘솔에서 처리해 달라"를 보여주고 메시지는 그대로 둔다.

## 7. 인수조건

- [x] 신고 트랜잭션이 롤백되면 알림이 가지 않고, 커밋되면 간다.
- [x] Slack 전송이 실패해도 신고는 저장되고 신고 API는 201이다.
- [x] 같은 대상의 신고는 묶음 시간 안에 한 번만 알리고, 시간이 지나면 다시 알린다. 다른 대상은 따로 알린다.
- [x] 같은 신고자의 재신고는 알리지 않는다.
- [x] 메시지에 신고자 정보와 본문이 없다.
- [x] Slack 처리: OPERATOR·ADMIN은 처리, VIEWER와 미연결 사용자는 403, 서명 오류·만료는 401.
- [x] Slack 처리 API는 API 키와 콘솔 세션을 받지 않고, 다른 관리자 API는 서명을 받지 않는다.
- [x] 처리자(`resolved_by`)와 작업 이력(`actor_admin_id`)에 누른 관리자가 남는다.
- [x] 이미 처리된 신고는 `ALREADY_RESOLVED`로 마지막 결과를 돌려주고 다시 처리하지 않는다.
- [ ] dev에서 실제 Slack 메시지와 버튼으로 숨김·기각을 확인한다.

## 8. 영향 범위 / 배포

- 마이그레이션 V29(NULL 허용 컬럼).
- 새 설정은 모두 기본값이 꺼짐이라, 설정하지 않으면 동작이 바뀌지 않는다. 바뀌는 기존 동작은 숨김·기각의 잠금 하나다.
- dev 실험 순서
  1. dev 서버 설정: `REPORT_ALERT_ENABLED`, `REPORT_ALERT_WEBHOOK_URL`, `REPORT_ALERT_CONSOLE_BASE_URL`, `REPORT_ALERT_BUTTONS_ENABLED`, `ADMIN_SLACK_ACTION_SECRET`
  2. dev DB: 관리자 계정에 `slack_user_id` 연결(SQL)
  3. 봇 설정: `REPORT_ACTIONS_ENABLED`, `REPORT_ACTION_BASE_URL`(dev), `REPORT_ACTION_SECRET`, `REPORT_ACTION_CHANNEL_IDS`
  4. Slack 앱: 웹훅은 **mody-bot 앱에서 만든 것**이어야 버튼 클릭이 봇으로 온다. 앱 설정에서 Interactivity를 켠다(Socket Mode라 Request URL은 필요 없다).
- 운영 적용 전 정할 것: 봇의 운영 변경 제한(`ENABLE_PROD_WRITES`, 서버 담당자 한정)과의 관계(§9). 그 전까지 봇은 운영 처리 요청을 거절한다.

### 배포 후 측정

- 접수부터 처리까지 시간(`resolved_at - created_at`)의 중앙값·최대값과 대기 신고 수. 기준선은 처리 0건, 최장 대기 117일.
- 처리 경로 비율: `admin_audit_log.request_path`가 `/api/v1/admin/slack/`인 처리 대 콘솔 처리.
- 알림 실패 수: 서버 로그 `report alert failed`.
- 묶여서 생략된 알림 수: 서버 로그 `report alert grouped`.

## 9. 미결정 사항 (Open Questions)

> ⚠️ 결정되지 않은 항목. `docs/policy/policy-checklist.md`의 "신고 처리"에도 같은 목록이 있다.

- [ ] 알림 채널: 서버 에러 채널과 나눌지 (설정만 바꾸면 된다)
- [ ] 묶음 시간 (지금 10분)
- [ ] 버튼 처리 권한 범위: 콘솔과 같은 OPERATOR 이상인지, 숨김은 ADMIN만인지
- [ ] 숨김 시 작성자에게 알릴지 (지금은 알리지 않음, 콘솔과 같음)
- [ ] 신고 사유 수집 (앱 변경 필요)
- [ ] 메시지에 작성자 닉네임을 넣을지 (지금은 넣음, 신고자는 넣지 않음)
- [ ] 운영 적용 시 봇 안전장치와의 관계: 버튼 처리를 `ENABLE_PROD_WRITES`·서버 담당자 제한에 묶을지, 관리자 역할 검사로 대신할지
- [ ] 대기 신고 정기 요약(알림 유실 보완)을 둘지

## 10. 참고

- [LLD-0019](LLD-0019-notification-dispatch-outbox.md), [LLD-0029](LLD-0029-server-error-slack-alert.md),
  [LLD-0037](LLD-0037-admin-operations-menu.md), [LLD-0039](LLD-0039-admin-comments-groups.md)
