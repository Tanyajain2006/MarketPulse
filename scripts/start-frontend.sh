#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"

if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
  echo "Error: Node.js 20+ and npm are required to start the frontend." >&2
  exit 1
fi

if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
  echo "Error: frontend dependencies are missing. Run 'npm install' in frontend/ first." >&2
  exit 1
fi

export VITE_API_BASE_URL="${VITE_API_BASE_URL:-http://localhost:8080/api}"
echo "Starting MarketPulse frontend with VITE_API_BASE_URL=$VITE_API_BASE_URL"
cd "$FRONTEND_DIR"
exec npm run dev
