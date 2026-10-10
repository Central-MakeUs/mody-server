#!/bin/bash
# 운영 서버로 가는 읽기 전용 SSH 터널. Grafana/Prometheus는 로컬에서만 돈다.
#   13306 -> mody-mysql:3306        (비즈니스 대시보드)
#   18081 -> mody-web-blue:8081     (성능 대시보드, actuator 관리 포트)
#
# 관리 포트는 운영 호스트에 공개돼 있지 않아 컨테이너 IP로 직접 포워딩한다.
set -euo pipefail

HOST=${MODY_SSH_HOST:-mody-prod}

app_ip=$(ssh -o BatchMode=yes "$HOST" \
  'docker inspect mody-web-blue --format "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}"')
mysql_ip=$(ssh -o BatchMode=yes "$HOST" \
  'docker inspect mody-mysql --format "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}"')

echo "app=$app_ip:8081 -> localhost:18081"
echo "mysql=$mysql_ip:3306 -> localhost:13306"
echo "종료하려면 Ctrl+C"

exec ssh -N \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=30 \
  -L "18081:${app_ip}:8081" \
  -L "13306:${mysql_ip}:3306" \
  "$HOST"
