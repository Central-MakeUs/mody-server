# 모니터링

운영 지표는 **운영 서버(Oracle A1)에 상주하는 스택**에서 본다(2026-10-17부터).

```
monitoring/
├── grafana/dashboards/      # 대시보드 2종 (비즈니스, 성능)
├── server/                  # 운영 서버 상주 스택 (기본)
│   ├── docker-compose.yml   # Prometheus, Grafana, mysqld-exporter, node-exporter
│   ├── prometheus.yml       # blue/green 앱, MySQL, 호스트를 15~30초 주기로 수집
│   └── provisioning/
└── local/                   # 예전 로컬 실행용 스택 (GCP e2-micro 시절)
```

## 접속

```bash
ssh -N -L 3000:127.0.0.1:3000 mody-prod   # 터널을 열어 둔 채로
open http://localhost:3000                 # Grafana (admin 비밀번호는 서버 ~/monitoring/server/monitoring.env)
```

Grafana와 Prometheus는 서버의 `127.0.0.1`에만 열려 있어 외부에서 직접 접근할 수 없다.

## 서버 배포

```bash
scp -r monitoring/server monitoring/grafana mody-prod:monitoring/
ssh mody-prod 'cd ~/monitoring/server && docker compose --env-file monitoring.env up -d'
```

`monitoring.env`는 `monitoring.env.example`을 복사해 서버에서만 채운다. 앱 compose 네트워크 `kminseok5167_mody-network`에 붙어 `mysql`, `mody-web-blue`, `mody-web-green`을 이름으로 수집한다.

## 예전 구성: 로컬 스택

아래는 운영이 GCP e2-micro(964MB)였을 때의 기록이다.

### 왜 로컬이었나

운영 서버는 RAM 964MB에 vCPU 0.25개(버스트)짜리다. Prometheus + Grafana + exporter를
올렸더니 메모리가 임계점을 넘겨 스왑 쓰래싱이 발생했다.

| 지표 | 모니터링 적재 중 | 제거 후 |
| --- | --- | --- |
| I/O 대기 | 76% | 16% |
| load average | 12.4 | 0.5 |
| `/actuator/prometheus` 응답 | 24초 | 0.6초 |
| 애플리케이션 응답 | 60초 타임아웃 | 0.4초 |

애플리케이션이 응답하지 못하는 상태가 되어 즉시 걷어냈다. 서버를 증설하기 전까지는
로컬에서 보는 구성을 쓴다.

## 실행

```bash
cd monitoring/local
cp local.env.example local.env   # 값 채우기 (운영 DB 읽기 전용 계정)

./tunnel.sh                      # 터미널 1: SSH 터널 유지
docker compose -f docker-compose.local.yml --env-file local.env up -d   # 터미널 2
open http://localhost:3000       # admin / local.env 의 비밀번호
```

터널은 운영 컨테이너 IP로 직접 포워딩한다. 운영 호스트에 포트를 추가로 열지 않는다.

| 로컬 포트 | 대상 | 용도 |
| --- | --- | --- |
| 13306 | `mody-mysql:3306` | 비즈니스 대시보드 |
| 18081 | `mody-web-blue:8081` | 성능 대시보드 (actuator) |

## 대시보드

- **MODY 비즈니스** — 가입·탈퇴, DAU, 누적 회원, 온보딩 완주율, 로그인 수단, 기록 작성,
  챌린지 생성·완주율, 인기 챌린지, 그룹, 알림 성과. 운영 MySQL을 직접 조회한다.
- **MODY 성능** — 처리량, 상태 코드, p50/p95/p99, 느린 엔드포인트, JVM 힙·GC·스레드,
  HikariCP, MySQL QPS·커넥션·버퍼 적중률.

호스트 자원(CPU·메모리·디스크) 패널은 node_exporter가 서버에서 돌아야 해서 제외했다.

## 운영 서버에 만들어 둔 계정

```sql
CREATE USER 'grafana'@'%'  IDENTIFIED BY '...';  GRANT SELECT ON mody.* TO 'grafana'@'%';
CREATE USER 'exporter'@'%' IDENTIFIED BY '...';  GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
```

## 주의

운영 서버 CPU가 느려 `/actuator/prometheus` 스크랩이 부하가 된다. 스크랩 주기는 5분으로
두었다. 더 자주 긁지 말 것.
