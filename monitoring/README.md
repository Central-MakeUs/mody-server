# 모니터링

운영 지표와 로그는 **운영 서버(Oracle A1, 24GB)에 상주하는 스택**에서 본다(2026-10-17부터).

```
monitoring/
├── grafana/dashboards/      # 대시보드 3종 (비즈니스, 성능, 사용자 지표)
├── server/                  # 운영 서버 상주 스택 (기본)
│   ├── docker-compose.yml   # Prometheus, Grafana, Loki, Alloy, mysqld-exporter, node-exporter
│   ├── prometheus.yml       # blue/green 앱·호스트 15초, MySQL 30초 주기 수집
│   ├── loki.yml             # 로그 저장소 (파일시스템 TSDB v13, 14일 보관)
│   ├── alloy.alloy          # 앱 컨테이너·호스트 nginx 로그 수집
│   ├── monitoring.env.example
│   └── provisioning/        # 데이터소스(Prometheus·Loki·MySQL) 및 대시보드 프로비저닝
└── local/                   # 예전 로컬 실행용 스택 (GCP e2-micro 시절)
```

## 접속

**https://prod-mody.shop/grafana/** (admin 비밀번호는 서버 `~/monitoring/server/monitoring.env`)

- nginx `location /grafana/`가 `127.0.0.1:3000`으로 넘긴다. 익명 접근과 회원가입은 꺼져 있다.
- 팀 공유용 조회 전용 계정 `mody-team`(Viewer)이 있다. 비밀번호는 서버 `monitoring.env`의 `GRAFANA_VIEWER_PASSWORD`.
- Prometheus(`127.0.0.1:9090`)와 Loki(`127.0.0.1:3100`)는 서버 로컬에만 열려 있다. 필요하면 `ssh -N -L 9090:127.0.0.1:9090 mody-prod`로 본다.

## 대시보드

`monitoring/grafana/dashboards/`의 JSON 3종이 Grafana `MODY` 폴더로 자동 프로비저닝된다(60초 갱신).

- **MODY 비즈니스** (`mody-business.json`) — 전체 회원·오늘 가입·DAU·활성 그룹, 일별 가입/탈퇴·누적 회원·로그인 수단·헬스 연동·온보딩 완주율·그룹 가입/평균 인원·7일 재방문율, 일별 기록·그룹 챌린지 생성/완료·완주율·인증 제출·댓글 수·인기 걸음수/사진 챌린지 Top 10·활동 기록 유형, 일별 알림 발송 및 유형별 성과. 운영 MySQL을 직접 조회한다.
- **MODY 성능** (`mody-performance.json`) — 애플리케이션 상태·가동 시간·처리량·5xx 에러율·p95 지연(히스토그램 버킷 기준), 상태 코드별 처리량·응답 시간 분위수·느린 엔드포인트 Top 10·5xx 에러 엔드포인트, JVM(힙·GC·스레드·CPU·HikariCP), MySQL(QPS·커넥션·InnoDB 버퍼 풀 적중률). Prometheus(TSDB 90일 보관)를 조회한다.
- **MODY 사용자 지표** (`mody-users.json`) — 관리 화면 `/admin/stats`와 같은 정의(식사·운동 기록, 챌린지 인증, 댓글)로 DAU/WAU/MAU, DAU/MAU, 일별 활성 사용자, 최근 30일 가입 퍼널, 주간 코호트 리텐션(%)을 보여준다.

## 로그 (Loki)

Grafana **Explore → Loki**에서 본다. Alloy가 Docker 소켓으로 앱 컨테이너(`mody-web-*`) 로그를, `/var/log/nginx/`에서 호스트 nginx 로그를 수집한다. 앱 로그는 블루그린 배포로 컨테이너가 바뀌어도 14일(`336h`) 동안 남으며, 멀티라인 스택트레이스는 첫 줄에 합쳐져 저장된다.

| 라벨 | 값 |
| --- | --- |
| `service` | `mody-server`, `nginx` |
| `container` | `mody-web-blue`, `mody-web-green` |
| `level` | `INFO`, `WARN`, `ERROR` (앱만) |
| `stream` | `access`, `error` (nginx만) |
| `env` | `prod` |

앱 로그 한 줄에는 `rid`(요청 ID)와 `mid`(회원 ID)가 붙는다. 응답 헤더 `X-Request-Id`가 `rid`와 같고, Grafana Loki 데이터소스의 `derivedFields`로 `rid`를 클릭해 같은 요청의 로그를 바로 모아 볼 수 있다.

```logql
{service="mody-server", level="ERROR"}                       # 에러
{service="mody-server"} |= "mid=123456789"                   # 특정 회원의 요청 흐름
{service="mody-server"} |= "rid=1a2b3c4d"                    # 한 요청의 로그 전부
{service="mody-server"} |= "access" |~ "status=(4|5)\\d\\d"  # 실패한 요청
{service="mody-server"} |= "Validation failed"               # 입력 검증 실패 (필드·거절값 포함)
{service="mody-server"} |= "image variant failed"            # 사진 축소본 비동기 생성 실패
{service="nginx", stream="access"} |~ "\" 5\\d\\d "          # nginx 5xx
```

주요 이벤트 로그: `login`, `member signed up`, `member withdrawn`, `record created/deleted`,
`comment created/deleted/reported`, `group created/joined/left`, `record reported`,
`weekly challenge proved`, `presigned url issued`, `image variant ready/failed`,
`push token registered/disabled`, `notification sent`.

## 서버 배포

```bash
scp -r monitoring/server monitoring/grafana mody-prod:monitoring/
ssh mody-prod 'cd ~/monitoring/server && docker compose --env-file monitoring.env up -d'
```

`monitoring.env`는 `monitoring.env.example`을 복사해 서버에서만 채운다. 앱 compose 네트워크 `kminseok5167_mody-network`에 붙어 `mysql`, `mody-web-blue`, `mody-web-green`을 이름으로 수집한다(블루그린 중 정지된 색상은 Prometheus에 `down`으로 남는다).

## 운영 DB 계정

Grafana MySQL 데이터소스와 `mysqld-exporter`는 운영 MySQL의 전용 계정을 쓴다.

```sql
CREATE USER 'grafana'@'%'  IDENTIFIED BY '...';  GRANT SELECT ON mody.* TO 'grafana'@'%';
CREATE USER 'exporter'@'%' IDENTIFIED BY '...';  GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
```

---

## 예전 구성: 로컬 스택 (GCP e2-micro 시절, 보관용)

아래는 2026-10-17 Oracle A1 이전 전, 운영 서버가 GCP e2-micro(RAM 964MB, vCPU 0.25 버스트)였을 때 쓰던 로컬 실행 구성 기록이다. 현재 운영에는 `monitoring/server/`를 쓴다.

### 왜 로컬이었나

GCP e2-micro에 Prometheus + Grafana + exporter를 올렸더니 메모리가 임계점을 넘겨 스왑 쓰래싱이 발생했다.

| 지표 | 모니터링 적재 중 | 제거 후 |
| --- | --- | --- |
| I/O 대기 | 76% | 16% |
| load average | 12.4 | 0.5 |
| `/actuator/prometheus` 응답 | 24초 | 0.6초 |
| 애플리케이션 응답 | 60초 타임아웃 | 0.4초 |

애플리케이션이 응답하지 못하는 상태가 되어 즉시 걷어내고, Oracle A1으로 이전하기 전까지 로컬 스택을 사용했다.

### 로컬 스택 실행 방법

```bash
cd monitoring/local
cp local.env.example local.env   # 값 채우기 (운영 DB 읽기 전용 계정)

./tunnel.sh                      # 터미널 1: SSH 터널 유지 (컨테이너 IP 변경 감지 재연결)
docker compose -f docker-compose.local.yml --env-file local.env up -d   # 터미널 2
open http://localhost:3000       # admin / local.env 의 비밀번호
```

터널은 운영 컨테이너 IP로 직접 포워딩한다. 운영 호스트에 포트를 추가로 열지 않는다.

| 로컬 포트 | 대상 | 용도 |
| --- | --- | --- |
| 13306 | `mody-mysql:3306` | 비즈니스 대시보드 |
| 18081 | `mody-web-blue:8081` | 성능 대시보드 (actuator) |

### 로컬 스택 제약 사항 (당시 기준)

- 호스트 자원(CPU·메모리·디스크)은 `node-exporter`를 서버에 띄우지 않아 제외했다.
- 당시 운영 서버 CPU 부하를 줄이기 위해 `monitoring/local/prometheus.yml`의 스크랩 주기를 5분으로 두었다.
