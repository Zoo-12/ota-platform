#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULT_DIR="$SCRIPT_DIR/results"
mkdir -p "$RESULT_DIR"

echo "=== OTA Platform 성능 테스트 ==="
echo ""

echo "[1/3] 요금 조회 캐시 효과 테스트"
k6 run --out json="$RESULT_DIR/rate-query-cache.json" \
  "$SCRIPT_DIR/rate-query-cache.js" 2>&1 | tee "$RESULT_DIR/rate-query-cache.txt"

echo ""
echo "[2/3] 숙소 검색 부하 테스트"
k6 run --out json="$RESULT_DIR/search-load.json" \
  "$SCRIPT_DIR/search-load.js" 2>&1 | tee "$RESULT_DIR/search-load.txt"

echo ""
echo "[3/3] 동시 예약 테스트"
k6 run --out json="$RESULT_DIR/concurrent-booking.json" \
  "$SCRIPT_DIR/concurrent-booking.js" 2>&1 | tee "$RESULT_DIR/concurrent-booking.txt"

echo ""
echo "=== 완료. 결과: $RESULT_DIR ==="
