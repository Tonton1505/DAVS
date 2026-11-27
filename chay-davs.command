#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================================="
echo "  Đang khởi động DAVS trên macOS / Linux (5173)   "
echo "=================================================="

if ! command -v node >/dev/null 2>&1; then
  echo "[LỖI] Chưa cài đặt Node.js (>= 20). Cài nhanh trên macOS: brew install node"
  exit 1
fi

node scripts/run-frontend.mjs ensure-deps

if command -v open >/dev/null 2>&1; then
  (sleep 2 && open "http://localhost:5173") &
elif command -v xdg-open >/dev/null 2>&1; then
  (sleep 2 && xdg-open "http://localhost:5173") &
fi

exec node scripts/run-frontend.mjs dev
