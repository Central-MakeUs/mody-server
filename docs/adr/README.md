# ADR (Architecture Decision Records)

중요한 아키텍처/기술 의사결정을 기록한다. 새 ADR은 `../templates/adr.md`를 복사해 `ADR-XXXX-<slug>.md`로 만든다.

| 번호 | 제목 | 상태 | 날짜 |
| --- | --- | --- | --- |
| [ADR-0001](ADR-0001-module-structure.md) | 모놀리식으로 시작하고 추후 모듈 추출 | Accepted | 2026-06-23 |
| [ADR-0002](ADR-0002-stable-stack-spring-boot-3.md) | Spring Boot 3.4.x 안정 스택 채택 (Boot 4 미사용) | Accepted | 2026-06-23 |
| [ADR-0003](ADR-0003-db-entity-conventions.md) | DB 및 엔티티 설계 기준 채택 | Accepted | 2026-06-23 |
| [ADR-0004](ADR-0004-entity-domain-unification.md) | 엔티티와 도메인 모델 통합 | Accepted | 2026-06-29 |
| [ADR-0005](ADR-0005-oauth-strategy-pattern.md) | 소셜 로그인 전략 패턴 도입 | Accepted | 2026-06-29 |
| [ADR-0006](ADR-0006-test-double-strategy.md) | 테스트 시 Fake와 Mock 사용 전략 | Accepted | 2026-06-29 |
| [ADR-0007](ADR-0007-cursor-pagination-strategy.md) | 커서 기반 페이징 전략 | Accepted | 2026-06-29 |
| [ADR-0008](ADR-0008-deployment-strategy.md) | 배포 전략 | Accepted | 2026-06-29 |
| [ADR-0009](ADR-0009-member-authentication-authorization.md) | 회원 인증/인가 전략 | Accepted | 2026-06-29 |
| [ADR-0010](ADR-0010-direct-image-upload-via-signed-url.md) | Signed URL 기반 직접 이미지 업로드 채택 | Accepted | 2026-06-29 |
| [ADR-0011](ADR-0011-app-entry-flow-state-flags.md) | 앱 진입 플로우 상태 플래그 분리 | Accepted | 2026-07-02 |
| [ADR-0012](ADR-0012-notification-transactional-outbox.md) | 알림 발송에 트랜잭셔널 아웃박스 패턴 채택 | Accepted | 2026-07-04 |
| [ADR-0013](ADR-0013-api-state-ownership.md) | API 상태 소유권 분리 | Accepted | 2026-07-04 |
| [ADR-0014](ADR-0014-record-streak-calculation.md) | 연속 기록 일수 계산 전략 | Accepted | 2026-07-04 |
| [ADR-0015](ADR-0015-group-member-unread-record-count.md) | 그룹원별 미확인 기록 수 계산 전략 | Accepted | 2026-07-04 |
| [ADR-0016](ADR-0016-record-detail-carousel-paging.md) | 기록 상세 캐러셀 API 페이징 전략 | Accepted | 2026-07-04 |
| [ADR-0018](ADR-0018-notification-batch-scheduler.md) | 알림 배치와 스케줄러 운영 전략 | Accepted | 2026-07-04 |
