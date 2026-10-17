#!/bin/bash
# 운영 서버로 가는 읽기 전용 SSH 터널. Grafana/Prometheus는 로컬에서만 돈다.
#   13306 -> mody-mysql:3306        (비즈니스 대시보드)
#   18081 -> mody-web-blue:8081     (성능 대시보드, actuator 관리 포트)
#
# 관리 포트는 운영 호스트에 공개돼 있지 않아 컨테이너 IP로 직접 포워딩한다.
# 배포로 컨테이너가 재생성되면 IP가 바뀌므로, 2분마다 확인해 바뀌면 다시 연결한다.
set -uo pipefail

HOST=${MODY_SSH_HOST:-mody-prod}
CHECK_INTERVAL=${MODY_TUNNEL_CHECK_INTERVAL:-120}

container_ip() {
  ssh -o BatchMode=yes -o ConnectTimeout=15 "$HOST" \
    "docker inspect $1 --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'" 2>/dev/null
}

ssh_pid=""
cleanup() { [ -n "$ssh_pid" ] && kill "$ssh_pid" 2>/dev/null; exit 0; }
trap cleanup INT TERM

while true; do
  app_ip=$(container_ip mody-web-blue)
  mysql_ip=$(container_ip mody-mysql)

  if [ -z "$app_ip" ] || [ -z "$mysql_ip" ]; then
    echo "[$(date +%H:%M:%S)] 컨테이너 IP를 읽지 못했다. 30초 후 재시도."
    sleep 30
    continue
  fi

  echo "[$(date +%H:%M:%S)] 연결: app=$app_ip:8081 -> 18081, mysql=$mysql_ip:3306 -> 13306"
  ssh -N -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=3 \
      -L "18081:${app_ip}:8081" -L "13306:${mysql_ip}:3306" "$HOST" &
  ssh_pid=$!

  # IP가 바뀌거나 ssh가 끊기면 다시 연결한다.
  while kill -0 "$ssh_pid" 2>/dev/null; do
    sleep "$CHECK_INTERVAL"
    new_app=$(container_ip mody-web-blue)
    new_mysql=$(container_ip mody-mysql)
    if [ -n "$new_app" ] && { [ "$new_app" != "$app_ip" ] || [ "$new_mysql" != "$mysql_ip" ]; }; then
      echo "[$(date +%H:%M:%S)] 컨테이너 IP 변경 감지. 터널을 다시 연결한다."
      kill "$ssh_pid" 2>/dev/null
      break
    fi
  done
  wait "$ssh_pid" 2>/dev/null
  sleep 3
done
