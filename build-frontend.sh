#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONT_DIR="$SCRIPT_DIR/../ota-platform-front"
STATIC_DIR="$SCRIPT_DIR/src/main/resources/static"

if [ ! -d "$FRONT_DIR" ]; then
  echo "❌ 프론트엔드 프로젝트를 찾을 수 없습니다: $FRONT_DIR"
  exit 1
fi

echo "=== [1/3] 프론트엔드 정적 빌드 ==="
cd "$FRONT_DIR"
source ~/.nvm/nvm.sh 2>/dev/null || true
nvm use 20 2>/dev/null || true
npm run build

echo ""
echo "=== [2/3] 기존 static 폴더 정리 ==="
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"

echo ""
echo "=== [3/3] 빌드 결과 복사 ==="
cp -r "$FRONT_DIR/out/"* "$STATIC_DIR/"

echo ""
echo "✅ 완료! Spring Boot 실행 시 http://localhost:8080 에서 프론트엔드 접근 가능"
