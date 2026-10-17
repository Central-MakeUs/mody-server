# 운영 배포 (블루그린)

운영 서버: Oracle Cloud A1 (ap-osaka-1, 4 OCPU / 24GB), SSH 별칭 `mody-prod`.

- `main`에 머지하면 **CD Prod with Gradle**이 자동 실행된다. 수동 실행 시 `deploy`를 끄면 이미지만 올린다.
- 워크플로는 amd64/arm64 이미지를 올린 뒤 `deploy.sh`를 서버 `~/mody-deploy/`에 올려 실행한다.

## 서버 구성

| 항목 | 위치 |
| --- | --- |
| compose | `~/docker-compose.yml` (시크릿 포함, 저장소에 두지 않음) |
| 활성 색상 | `~/mody-deploy/state/active-color` → 컨테이너 `/run/mody/active-color` (읽기 전용) |
| nginx upstream | `/etc/nginx/conf.d/mody-upstream.conf` (`upstream mody_app`) |
| DB 백업 | `~/mody-deploy/backup-db.sh`, 매일 04:00 → `gs://mody-db-backups/prod/` (30일 보관) |

| 색상 | 컨테이너 | 포트 | `ID_NODE_ID` |
| --- | --- | --- | --- |
| blue | `mody-web-blue` | 8080 | 1 |
| green | `mody-web-green` | 8082 | 2 |

공통 환경 변수는 `~/app.env`(권한 600)에 두고, 서비스별로는 색상과 `ID_NODE_ID`만 다르다. 환경 변수를 추가할 때는 `app.env`에 넣는다.

## 명령

```bash
~/mody-deploy/deploy.sh status    # 활성 색상, upstream, 컨테이너 상태
~/mody-deploy/deploy.sh deploy    # 새 이미지로 반대 색상 기동 후 전환
~/mody-deploy/deploy.sh rollback  # 정지해 둔 이전 색상으로 되돌림
```

## 주의

- 전환하는 동안 두 버전이 같은 DB를 쓴다. 마이그레이션은 하위 호환으로 작성한다(컬럼 삭제와 이름 변경은 두 번에 나눈다).
- 스케줄 작업은 활성 색상 인스턴스에서만 돈다(`ActiveColorSchedulingGate`).
- 어드민 세션은 인스턴스 메모리에 있으므로 색상이 바뀌면 다시 로그인해야 한다.
- 2026-10-11 GCP e2-micro에서 블루그린을 시도했다가 메모리 부족으로 장애가 났다(#224). 메모리가 작은 서버로 옮길 때는 다시 검토한다.
