#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "=== [1/3] Docker 인프라 기동 ==="
docker compose -f "$PROJECT_ROOT/docker-compose.yml" up -d

echo ""
echo "=== [2/3] MySQL / Redis ready 대기 ==="

wait_healthy() {
  local container=$1
  local max=30
  local i=0
  echo -n "Waiting for $container"
  while [ $i -lt $max ]; do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "starting")
    if [ "$status" = "healthy" ]; then
      echo " OK"
      return 0
    fi
    echo -n "."
    sleep 2
    i=$((i + 1))
  done
  echo ""
  echo "ERROR: $container did not become healthy in time."
  exit 1
}

wait_healthy ota-mysql
wait_healthy ota-redis

echo ""
echo "=== [3/3] Spring Boot 서버 기동 (profile: local) ==="
cd "$PROJECT_ROOT"
./gradlew bootRun --args='--spring.profiles.active=local'
