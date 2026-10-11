#!/usr/bin/env bash
# 운영 배포 (단일 인스턴스).
#
# 운영 서버(메모리 964MB)는 JVM 두 개를 동시에 띄우면 swap 스래싱으로 기존 인스턴스까지 멈춘다
# (2026-10-11 블루그린 시도 중 장애). 그래서 기존 컨테이너를 내리고 새로 띄우며, 기동하는 동안
# 다운타임이 생긴다. 트래픽이 적은 시간에 수동으로 실행한다.
#
# 사용법: deploy.sh [deploy|rollback|status]
set -euo pipefail

COMPOSE_DIR="${COMPOSE_DIR:-$HOME}"
SERVICE=web-blue
CONTAINER=mody-web-blue
IMAGE="${IMAGE:-minseok5167/mody-server}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-900}"

log() { echo "[deploy $(date '+%H:%M:%S')] $*"; }
compose() { docker compose --project-directory "$COMPOSE_DIR" "$@"; }

wait_healthy() {
    local deadline=$(( $(date +%s) + HEALTH_TIMEOUT_SECONDS ))
    log "기동 대기 (최대 ${HEALTH_TIMEOUT_SECONDS}s)"
    while [ "$(date +%s)" -lt "$deadline" ]; do
        if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)" != true ]; then
            log "$CONTAINER 컨테이너가 멈췄습니다"
            return 1
        fi
        if curl -fsS -o /dev/null --max-time 5 http://127.0.0.1:8080/health; then
            log "정상 응답"
            return 0
        fi
        sleep 5
    done
    log "기동 시간 초과"
    return 1
}

restart_with() {
    compose up -d --no-deps --force-recreate "$SERVICE"
    if ! wait_healthy; then
        docker logs --tail 80 "$CONTAINER" || true
        return 1
    fi
}

deploy() {
    # 지금 이미지를 previous 로 남겨 rollback 에 쓴다.
    local current
    current="$(docker inspect -f '{{.Image}}' "$CONTAINER" 2>/dev/null || true)"
    [ -n "$current" ] && docker tag "$current" "$IMAGE:previous"

    compose pull "$SERVICE"
    restart_with || { log "배포 실패: '$0 rollback' 으로 되돌릴 수 있습니다"; exit 1; }
    docker image prune -f > /dev/null
    log "배포 완료"
}

rollback() {
    docker image inspect "$IMAGE:previous" > /dev/null
    docker tag "$IMAGE:previous" "$IMAGE:latest"
    restart_with
    log "rollback 완료"
}

status() {
    docker ps -a --filter "name=$CONTAINER" --format '{{.Names}}\t{{.Status}}\t{{.Image}}'
    docker images "$IMAGE" --format '{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}'
}

case "${1:-deploy}" in
    deploy) deploy ;;
    rollback) rollback ;;
    status) status ;;
    *) echo "usage: $0 [deploy|rollback|status]" >&2; exit 2 ;;
esac
