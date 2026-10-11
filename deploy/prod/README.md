# 운영 배포

운영 배포는 다운타임이 있는 단일 인스턴스 방식이다. 기동에 3~10분이 걸리므로 트래픽이 적은 밤에 실행한다.

- `main` 머지만으로는 배포되지 않는다. Actions의 **CD Prod with Gradle**을 수동 실행한다.
  ```bash
  gh workflow run deploy-prod.yml --ref main
  ```
- 워크플로는 이미지를 빌드해 올린 뒤 `deploy.sh`를 서버 `~/mody-deploy/`에 올려 실행한다.

## 블루그린을 쓰지 않는 이유

2026-10-11에 같은 서버에서 블루그린을 시도했다(#224). JVM 두 개가 동시에 뜨자 메모리 964MB가 바닥나고 swap 스래싱이 일어났다. 기존 인스턴스까지 2분 넘게 멈춰 약 3분간 장애가 났다. 무중단 배포를 하려면 서버 메모리를 2GB 이상으로 올려야 한다.

## 명령

```bash
~/mody-deploy/deploy.sh status    # 컨테이너와 이미지(latest, previous) 상태
~/mody-deploy/deploy.sh deploy    # 새 이미지로 재기동, /health 대기
~/mody-deploy/deploy.sh rollback  # 직전 배포 이미지(previous)로 재기동
```
