# ADR-0008: 배포 전략

> Architecture Decision Record. 하나의 중요한 의사결정과 그 이유를 기록한다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| 날짜 | 2026-06-29 |
| 관련 | GitHub Actions, Docker, GCP Compute Engine |

## 맥락 (Context)

초기 단계에서는 관리형 Kubernetes나 복잡한 멀티 리전 배포보다 단순하고 재현 가능한 배포 파이프라인이
더 중요하다. 현재 서비스는 단일 Spring Boot API 서버와 MySQL 중심으로 구성되며,
운영 복잡도를 낮추면서도 CI, 이미지 빌드, 원격 서버 반영 과정을 자동화해야 한다.

## 결정 (Decision)

`main` 브랜치 머지 또는 직접 푸쉬를 기준으로 dev 환경 배포를 수행한다.
GitHub Actions에서 테스트와 빌드를 통과한 뒤 Docker 이미지를 생성하고,
GCP Compute Engine VM에 SSH로 접속해 Docker Compose 기반으로 애플리케이션을 갱신한다.

배포 원칙은 다음과 같다.

- 애플리케이션 설정은 환경변수로 주입한다.
- dev와 prod는 Docker Compose 환경변수와 GitHub Secrets로 분리한다.
- 빌드 산출물은 Docker 이미지로 고정하고 서버에서는 이미지를 pull 후 재기동한다.
- 배포 후 `/actuator/health` 또는 헬스 체크 API로 정상 기동을 확인한다.
- 서버에 직접 있는 compose 파일은 운영 환경별 인프라 설정으로 보고 저장소 코드와 분리한다.

## 고려한 대안 (Considered Options)

1. **VM 수동 배포** — 이해하기 쉽지만 재현성과 변경 추적이 떨어진다.
2. **GitHub Actions + SSH + Docker Compose** — 단순하고 현재 규모에 충분하다.
3. **Kubernetes 기반 배포** — 확장성은 높지만 초기 운영 비용과 학습 비용이 크다.

## 결과 (Consequences)

### 긍정

- main 기준 배포 흐름이 명확해진다.
- 로컬 빌드 결과와 서버 실행 단위를 Docker 이미지로 맞출 수 있다.
- dev와 prod 설정을 환경변수와 Secret으로 분리할 수 있다.
- 현재 규모에서 운영 부담을 낮게 유지한다.

### 부정 / 트레이드오프

- 단일 VM 장애에 취약하다.
- 무중단 배포와 롤백 자동화는 별도 설계가 필요하다.
- 서버의 Docker Compose 파일 변경 이력은 저장소만으로 완전히 추적되지 않는다.

## 후속 / 미결정

- blue/green 또는 rolling 배포 필요 시 compose 구성을 확장한다.
- prod 환경 생성 시 Secret 분리, 도메인, TLS, 백업 정책을 별도 ADR로 기록한다.
