# LLD-0007: 환경별 애플리케이션 설정 분기

> Low-Level Design. 이 문서는 `application.yaml` profile 분기와 PR 본문의 **오라클(ground truth)** 이다.

| 항목 | 값 |
| --- | --- |
| 상태 | Accepted |
| Issue | #22 |
| 관련 ADR | _(없음)_ |
| 작성자 | msk226 |
| 작성일 | 2026-06-28 |

## 1. 목적 / 배경

- 로컬, dev, prod 환경을 하나의 `application.yaml`에서 Spring profile로 분기한다.
- 로컬 개발은 별도 profile 주입 없이 `local`로 실행한다.
- Docker 배포 환경은 서버의 raw `docker-compose.yml`에서 `SPRING_PROFILES_ACTIVE`를 주입해 제어한다.
- repository의 `docker-compose.yml`은 배포 서버 raw 파일과 별개이므로 본 작업 범위에서 변경하지 않는다.

## 2. 범위

### In scope

- `src/main/resources/application.yaml`에 `local`, `dev`, `prod` profile 문서를 추가한다.
- 기본 active profile은 `local`로 둔다.
- 기존 `application-local.yaml` 내용을 단일 `application.yaml`로 이동한다.
- 외부/환경별 application yaml 파일이 커밋되지 않도록 `.gitignore`에 추가한다.

### Out of scope

- 배포 서버 raw `docker-compose.yml` 직접 수정.
- GitHub Actions 배포 workflow 변경.
- OAuth provider secret 값 확정.

## 3. 설정 구조

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
---
spring:
  config:
    activate:
      on-profile: local
---
spring:
  config:
    activate:
      on-profile: dev
---
spring:
  config:
    activate:
      on-profile: prod
```

## 4. 환경별 제어 방식

- local: profile을 주입하지 않으면 기본값 `local`이 적용된다.
- dev: 배포 서버 raw Docker Compose가 `SPRING_PROFILES_ACTIVE=dev`를 주입한다.
- prod: prod Docker Compose가 `SPRING_PROFILES_ACTIVE=prod`를 주입한다.
- DB 접속 정보, JWT secret, ID node id는 각 환경의 Docker Compose environment로 주입한다.

## 5. 인수조건 (Acceptance Criteria)

- [x] `application.yaml` 하나에서 `local`, `dev`, `prod` profile이 분기된다.
- [x] 기본 active profile은 `local`이다.
- [x] 기존 `application-local.yaml`은 제거된다.
- [x] 배포 서버 raw Docker Compose 변경은 repository에 포함하지 않는다.
- [x] 환경별 application yaml 파일은 `.gitignore`에 포함된다.
- [x] `./gradlew clean build`가 통과한다.

## 6. 영향 범위 / 마이그레이션

- 로컬 실행은 기존처럼 별도 profile 없이 가능하다.
- dev 배포 서버 raw Docker Compose에는 `SPRING_PROFILES_ACTIVE=dev`를 주입해야 한다.
- prod 배포 시 raw Docker Compose에는 `SPRING_PROFILES_ACTIVE=prod`를 주입해야 한다.

## 7. 미결정 사항 (Open Questions)

> 결정되지 않은 항목.

- [ ] prod Docker Compose 파일의 최종 위치와 관리 방식.
- [ ] prod의 `JPA_DDL_AUTO` 기본값을 compose environment로 강제할지 여부.
