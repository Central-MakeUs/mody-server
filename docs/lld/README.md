# LLD (Low-Level Design)

기능별 상세 설계 문서. **PR 본문의 오라클**로 사용된다. 새 LLD는 `../templates/lld.md`를 복사해 `LLD-XXXX-<slug>.md`로 만든다.

각 LLD에는 반드시 `## 미결정 사항(Open Questions)` 섹션을 둔다 — PR에서 빈칸으로 처리되는 근거.

| 번호 | 제목 | 상태 | Issue |
| --- | --- | --- | --- |
| [LLD-0001](LLD-0001-health-check.md) | 헬스 체크 API + 공통 응답/예외 처리 | Accepted | _(미발급)_ |
| [LLD-0002](LLD-0002-jwt-token-foundation.md) | JWT 토큰 기반 로직 | Accepted | _(미발급)_ |
| [LLD-0003](LLD-0003-common-foundation.md) | 공통 응답/예외 및 엔티티 기반 | Accepted | _(미발급)_ |
| [LLD-0004](LLD-0004-erd-entity-foundation.md) | ERD 및 엔티티 기반 설계 | Accepted | #1 |
| [LLD-0005](LLD-0005-oauth-social-login.md) | OAuth 소셜 로그인 | Accepted | #18 |
| [LLD-0006](LLD-0006-id-generator-foundation.md) | ID 생성기 기반 | Accepted | #19 |
| [LLD-0007](LLD-0007-application-profile-configuration.md) | 환경별 애플리케이션 설정 분기 | Accepted | #22 |
| [LLD-0008](LLD-0008-group-api.md) | 그룹 API | Accepted | #30 |
| [LLD-0009](LLD-0009-mypage-member-weight-api.md) | 마이페이지 회원 정보 및 체중 기록 API | Accepted | #32 |
| [LLD-0010](LLD-0010-auth-token-lifecycle.md) | 토큰 재발급 및 로그아웃 API | Accepted | #36 |
| [LLD-0011](LLD-0011-presigned-url-api.md) | Presigned URL 발급 API | Accepted | #38 |
| [LLD-0012](LLD-0012-activity-record-create-api.md) | 기록 업로드 API | Accepted | #40 |
| [LLD-0013](LLD-0013-activity-record-feed-api.md) | 기록 피드 조회 API | Accepted | #42 |
