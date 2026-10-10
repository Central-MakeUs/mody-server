# LLD-0034: 운영 모니터링 대시보드 (Grafana)

> Low-Level Design. 이 문서는 해당 기능 구현과 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #213 |
| 관련 ADR | - |
| 작성자 | msk226 |
| 작성일 | 2026-10-10 |

## 1. 목적 / 배경

운영 서버에 지표를 상시 확인할 수단이 없다. 현재 관측 수단은 두 가지뿐이다.

- 500 에러 발생 시 Slack 알림 (LLD-0029)
- 컨테이너 로그 직접 조회 (`docker logs mody-web-blue`)

둘 다 사후 대응이고, 지연 시간이 늘고 있다거나 커넥션 풀이 고갈되어 간다는 식의 **추세**는 볼 수 없다. 비즈니스 지표(가입자, 활성 사용자, 챌린지 완주율)도 필요할 때마다 DB에 직접 쿼리한다.

Grafana 대시보드 2종을 운영 서버에 올려 비즈니스 지표와 시스템 성능을 상시 확인한다.

## 2. 범위

### In scope
- 애플리케이션 메트릭 노출 (Actuator + Micrometer Prometheus)
- Prometheus, Grafana, mysqld_exporter, node_exporter 컨테이너 구성
- Grafana 데이터소스·대시보드 프로비저닝 (코드로 형상관리)
- 비즈니스 대시보드 1종 (MySQL 직접 쿼리)
- 성능 대시보드 1종 (Prometheus)
- Grafana 전용 MySQL 읽기 전용 계정

### Out of scope
- 알림 규칙(Alerting) 구성 — 500 에러는 기존 Slack 알림으로 충분하다고 보고 이번에는 제외한다.
- 로그 수집(Loki)과 분산 추적(Tempo) — 서버 자원이 부족하다.
- Grafana 외부 공개와 인증 연동 — SSH 터널로만 접근한다.
- dev 서버 적용 — dev 서버는 현재 중지 상태다.

## 3. 인터페이스 / API

### 애플리케이션 메트릭 엔드포인트

| 항목 | 값 |
| --- | --- |
| 포트 | `8081` (`management.server.port`) |
| 경로 | `/actuator/prometheus`, `/actuator/health`, `/actuator/info` |
| 노출 범위 | 도커 네트워크 내부 전용. compose에서 호스트 포트로 포워딩하지 않는다. |

서비스 포트(8080)와 관리 포트(8081)를 분리한 이유는, 8080이 `prod-mody.shop`으로 외부에 공개되어 있어 같은 포트에 두면 메트릭이 외부에 노출되기 때문이다.

### 모니터링 서비스 포트

| 서비스 | 바인딩 | 비고 |
| --- | --- | --- |
| Grafana | `127.0.0.1:3000` | `ssh -L 3000:localhost:3000 mody-prod`로 접근 |
| Prometheus | `127.0.0.1:9090` | 쿼리 디버깅용 |
| mysqld_exporter | 내부 전용 | |
| node_exporter | 내부 전용 | |

## 4. 데이터 모델

새 테이블이나 마이그레이션은 없다. 비즈니스 대시보드는 기존 테이블을 읽기만 한다.

### Grafana 전용 MySQL 계정

```sql
CREATE USER 'grafana'@'%' IDENTIFIED BY '<password>';
GRANT SELECT ON mody.* TO 'grafana'@'%';
```

`mysqld_exporter`는 별도 계정을 쓴다.

```sql
CREATE USER 'exporter'@'%' IDENTIFIED BY '<password>';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
```

애플리케이션은 `root`를 쓰고 있으나 이번 범위에서 바꾸지 않는다.

### 비즈니스 지표 소스

모든 엔티티가 `BaseEntity`를 상속해 `created_at`, `updated_at`, `deleted_at`, `status`를 가진다. 지표는 이 컬럼들을 기준으로 집계한다.

| 지표 | 테이블 |
| --- | --- |
| 가입자, 탈퇴자 | `member` (`created_at`, `deleted_at`, `status`) |
| 활성 사용자(DAU/WAU) | `activity_record`, `step_record`, `weight_record`의 `member_id` distinct |
| 온보딩 완주 | `member.group_onboarding_completed`, `health_connection_status` |
| 로그인 수단 | `social_account.login_type` |
| 기록 작성 | `activity_record`, `weight_record`, `step_record` |
| 챌린지 | `challenge`, `challenge_proof`, `step_challenge_detail` |
| 그룹 | `mody_group`, `group_member.status` |
| 소셜 반응 | `record_comment`, `record_view_history`, `record_report` |
| 알림 | `notification.delivery_status`, `notification_type` |

## 5. 처리 흐름

### 최초 설계와 실패 (2026-10-10)

모니터링 스택을 운영 서버에 올리는 것으로 설계해 배포했으나, **운영 서버가 버티지 못했다.**

서버 사양은 RAM 964MB, vCPU 0.25개(e2-micro 버스트)다. 컨테이너별 메모리 상한을 걸었는데도
전체 사용량이 임계점을 넘겨 스왑 쓰래싱이 발생했다.

| 지표 | 모니터링 적재 중 | 제거 후 |
| --- | --- | --- |
| I/O 대기 | 76.1% | 16.1% |
| load average | 12.4 | 0.47 |
| 스왑 사용 / `si` | 849MB / 700~1100KB·s | 799MB / 0 |
| `/actuator/prometheus` 응답 | 24.4초 | 0.6초 |
| 애플리케이션 응답 | 60초 타임아웃 | 0.42초 |
| HikariCP | `Thread starvation (housekeeper delta=1m5s)` | 정상 |

메모리가 원인이고 CPU 스틸은 1.1%에 불과했다. 스왑에서 페이지를 계속 읽느라 I/O 대기가
76%까지 올라갔고, 애플리케이션이 외부 요청에 응답하지 못했다. 즉시 스택을 중지하고
볼륨과 이미지까지 제거했다.

### 최종 구조: 로컬 실행

```
[개발자 맥]                                  [운영 서버]
Grafana  ─┐
          ├─ SSH 터널 13306 ──────────────→ mody-mysql:3306      (비즈니스)
Prometheus┤
          └─ SSH 터널 18081 ──────────────→ mody-web-blue:8081   (성능, 5분 주기)
mysqld_exporter ─ 13306 경유
```

- 운영 서버에서 도는 프로세스는 **없다**. 애플리케이션의 actuator 엔드포인트만 남는다.
- 터널은 운영 컨테이너 IP로 직접 포워딩한다. 운영 호스트에 포트를 추가로 열지 않는다.
- 비즈니스 대시보드는 MySQL을 직접 조회하고, 성능 대시보드는 로컬 Prometheus를 본다.
- 스크랩 주기는 5분이다. 운영 CPU가 느려 더 자주 긁으면 애플리케이션에 부담이 된다.
- 호스트 자원 패널은 node_exporter가 서버에서 돌아야 해서 제외했다.

## 6. 예외 / 에러 처리

| 상황 | 동작 |
| --- | --- |
| 애플리케이션 재시작 중 스크랩 실패 | Prometheus가 해당 타깃을 `down`으로 기록. 대시보드에 공백으로 표시된다. |
| Grafana 컨테이너 재생성 | 데이터소스와 대시보드는 프로비저닝 파일에서 복원된다. 수동 편집분은 유실된다. |
| Prometheus 디스크 증가 | 보관 15일, 크기 상한 1GB로 제한해 오래된 블록부터 삭제한다. |
| 메모리 부족 | 모든 모니터링 컨테이너에 `mem_limit`을 건다. 초과 시 해당 컨테이너만 OOM으로 종료되고 애플리케이션은 보호된다. |

## 7. 인수조건 (Acceptance Criteria)

- [ ] `curl localhost:8081/actuator/prometheus`가 운영 서버에서 메트릭을 반환한다.
- [ ] 외부에서 `https://prod-mody.shop/actuator/prometheus`가 접근되지 않는다.
- [ ] Prometheus에서 세 타깃이 모두 `up` 상태다.
- [ ] 성능 대시보드에서 HTTP 처리량·p95 지연·에러율·JVM 힙·HikariCP·MySQL·호스트 자원이 보인다.
- [ ] 비즈니스 대시보드에서 가입자·활성 사용자·기록·챌린지·그룹·알림 지표가 보인다.
- [ ] 모니터링 스택 기동 후에도 애플리케이션이 정상 응답하고 OOM이 발생하지 않는다.
- [ ] Grafana 컨테이너를 삭제 후 재생성해도 대시보드가 자동 복원된다.

## 8. 영향 범위 / 마이그레이션

| 대상 | 영향 |
| --- | --- |
| `build.gradle.kts` | actuator, micrometer-registry-prometheus 의존성 추가 |
| `application.yaml` | `management.*` 설정 추가. 관리 포트 8081 분리 |
| 운영 compose | 애플리케이션 서비스에 `MANAGEMENT_SERVER_PORT` 추가 (기본값이 있어 생략 가능) |
| 운영 서버 메모리 | **증가 없음.** 모니터링은 로컬에서 돈다. actuator 메트릭 레지스트리만 인메모리로 추가된다. |
| 배포 | main 푸시 시 기존 `deploy-prod.yml`로 애플리케이션 배포. 모니터링은 개발자 맥에서 `monitoring/local`로 띄운다. |

애플리케이션 동작 자체는 바뀌지 않는다. 메트릭 수집은 인메모리 집계이고 스크랩 시점에만 직렬화한다.

## 9. 미결정 사항 (Open Questions)

> ⚠️ 결정되지 않은 항목. PR 본문에서 빈칸으로 처리되며 확인 대상이 된다. 추측으로 채우지 말 것.

- [ ] **서버 증설 여부.** 현재 사양(RAM 964MB, vCPU 0.25)은 애플리케이션 하나로도 빠듯하다.
      기동에 463~584초가 걸린다. 증설하면 모니터링을 서버에 상주시키고 팀원과 공유할 수 있다.
- [ ] 팀원 공유 방법. 지금은 개발자 맥에서만 볼 수 있다.
- [ ] 알림 규칙 도입 범위. 로컬 Grafana는 항상 켜져 있지 않아 알림 주체로 적합하지 않다.
- [ ] 운영 MySQL 3306 포트가 외부(0.0.0.0)로 열려 있다. 방화벽 정리가 필요하다.
- [ ] 애플리케이션 DB 계정을 `root`에서 최소 권한 계정으로 교체할지.

## 10. 참고

- [LLD-0029](LLD-0029-server-error-slack-alert.md) — 500 에러 Slack 알림
- [Spring Boot Actuator 메트릭](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [Grafana 프로비저닝](https://grafana.com/docs/grafana/latest/administration/provisioning/)
