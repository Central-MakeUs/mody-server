#!/usr/bin/env bash
# 운영 블루그린 배포 (Oracle A1, 4 OCPU / 24GB).
#
# GCP e2-micro(964MB)에서는 JVM 두 개를 동시에 띄우면 swap 스래싱으로 장애가 났다(2026-10-11).
# Oracle로 옮긴 뒤(2026-10-17) 메모리가 충분해 다시 블루그린으로 배포한다.
#
# 1. 비활성 색상 컨테이너를 새 이미지로 띄운다 (스케줄 작업은 꺼진 상태로 뜬다).
# 2. /health 가 200 을 줄 때까지 기다린다. 실패하면 새 컨테이너를 내리고 끝낸다(기존 색상은 그대로).
# 3. nginx upstream 을 새 포트로 바꾸고 reload 한다.
# 4. 활성 색상 파일을 바꿔 스케줄 작업 실행 권한을 넘긴다.
# 5. 이전 색상을 graceful 하게 멈춘다. 컨테이너는 지우지 않아 rollback 할 수 있다.
#
# 사용법: deploy.sh [deploy|rollback|status]
set -euo pipefail

COMPOSE_DIR="${COMPOSE_DIR:-$HOME}"
STATE_DIR="${STATE_DIR:-$HOME/mody-deploy/state}"
ACTIVE_COLOR_FILE="$STATE_DIR/active-color"
UPSTREAM_CONF="${UPSTREAM_CONF:-/etc/nginx/conf.d/mody-upstream.conf}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-900}"

port_of() { [ "$1" = blue ] && echo 8080 || echo 8082; }
other_of() { [ "$1" = blue ] && echo green || echo blue; }
log() { echo "[deploy $(date '+%H:%M:%S')] $*"; }
compose() { docker compose --project-directory "$COMPOSE_DIR" "$@"; }

active_color() {
    if [ -s "$ACTIVE_COLOR_FILE" ]; then
        tr -d '[:space:]' < "$ACTIVE_COLOR_FILE"
    else
        echo blue
    fi
}

wait_healthy() {
    local color="$1" port deadline
    port="$(port_of "$color")"
    deadline=$(( $(date +%s) + HEALTH_TIMEOUT_SECONDS ))
    log "$color(:$port) 기동 대기 (최대 ${HEALTH_TIMEOUT_SECONDS}s)"
    while [ "$(date +%s)" -lt "$deadline" ]; do
        if [ "$(docker inspect -f '{{.State.Running}}' "mody-web-$color" 2>/dev/null)" != true ]; then
            log "mody-web-$color 컨테이너가 멈췄습니다"
            return 1
        fi
        if curl -fsS -o /dev/null --max-time 5 "http://127.0.0.1:$port/health"; then
            log "$color 정상 응답"
            return 0
        fi
        sleep 5
    done
    log "$color 기동 시간 초과"
    return 1
}

switch_traffic() {
    local color="$1" port tmp
    port="$(port_of "$color")"
    tmp="$(mktemp)"
    printf 'upstream mody_app {\n    server 127.0.0.1:%s;\n}\n' "$port" > "$tmp"
    sudo cp "$UPSTREAM_CONF" "$UPSTREAM_CONF.prev" 2>/dev/null || true
    sudo install -m 644 "$tmp" "$UPSTREAM_CONF"
    rm -f "$tmp"
    if ! sudo nginx -t -q; then
        log "nginx 설정 검증 실패, 이전 설정으로 되돌립니다"
        sudo install -m 644 "$UPSTREAM_CONF.prev" "$UPSTREAM_CONF"
        return 1
    fi
    sudo nginx -s reload
    mkdir -p "$STATE_DIR"
    printf '%s\n' "$color" > "$ACTIVE_COLOR_FILE.tmp"
    mv "$ACTIVE_COLOR_FILE.tmp" "$ACTIVE_COLOR_FILE"
    log "트래픽과 스케줄 작업을 $color(:$port) 로 전환"
}

deploy() {
    local current next
    current="$(active_color)"
    next="$(other_of "$current")"
    log "현재 $current → 새 버전 $next"

    compose pull "web-$next"
    compose up -d --no-deps --force-recreate "web-$next"

    if ! wait_healthy "$next"; then
        docker logs --tail 80 "mody-web-$next" || true
        compose stop "web-$next" || true
        log "배포 실패: $current 가 계속 서비스합니다"
        exit 1
    fi

    switch_traffic "$next"
    # nginx reload 후 이전 워커가 처리 중인 요청을 마칠 시간을 준다.
    sleep 5
    compose stop -t 30 "web-$current" || true
    docker image prune -f > /dev/null
    log "배포 완료: $next 서비스 중, $current 는 정지(rollback 대기)"
}

rollback() {
    local current previous
    current="$(active_color)"
    previous="$(other_of "$current")"
    log "rollback: $current → $previous"
    docker start "mody-web-$previous" > /dev/null
    wait_healthy "$previous"
    switch_traffic "$previous"
    sleep 5
    compose stop -t 30 "web-$current" || true
    log "rollback 완료: $previous 서비스 중"
}

status() {
    log "활성 색상: $(active_color)"
    sudo cat "$UPSTREAM_CONF" 2>/dev/null || true
    docker ps -a --filter name=mody-web --format '{{.Names}}\t{{.Status}}\t{{.Image}}'
}

case "${1:-deploy}" in
    deploy) deploy ;;
    rollback) rollback ;;
    status) status ;;
    *) echo "usage: $0 [deploy|rollback|status]" >&2; exit 2 ;;
esac
