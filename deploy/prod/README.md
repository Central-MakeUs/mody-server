# 운영 블루그린 배포

`deploy-prod.yml`이 `deploy.sh`를 서버의 `~/mody-deploy/`로 올린 뒤 실행한다.

## 서버 구성

| 항목 | 위치 |
| --- | --- |
| compose | `~/docker-compose.yml` (시크릿 포함, 저장소에 두지 않음) |
| 활성 색상 | `~/mody-deploy/state/active-color` → 컨테이너 `/run/mody/active-color` (읽기 전용) |
| nginx upstream | `/etc/nginx/conf.d/mody-upstream.conf` (`upstream mody_app`) |

| 색상 | 컨테이너 | 포트 | `ID_NODE_ID` |
| --- | --- | --- | --- |
| blue | `mody-web-blue` | 8080 | 1 |
| green | `mody-web-green` | 8082 | 2 |

두 서비스는 compose의 `x-web` 앵커로 환경 변수를 공유한다. 환경 변수를 추가할 때는 앵커에 넣는다.

## 명령

```bash
~/mody-deploy/deploy.sh status    # 활성 색상, upstream, 컨테이너 상태
~/mody-deploy/deploy.sh deploy    # 새 이미지로 반대 색상 기동 후 전환
~/mody-deploy/deploy.sh rollback  # 정지해 둔 이전 색상으로 되돌림
```

## 주의

- 전환하는 동안 두 버전이 같은 DB를 쓴다. 마이그레이션은 하위 호환으로 작성한다.
- 스케줄 작업은 활성 색상 인스턴스에서만 돈다(`ActiveColorSchedulingGate`).
