#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ML_DIR="$ROOT_DIR/ml-service"

if ! command -v python3 >/dev/null 2>&1; then
  echo "Error: Python 3.11+ is required to start the ML service." >&2
  exit 1
fi

if ! python3 -c 'import fastapi, uvicorn' >/dev/null 2>&1; then
  echo "Error: FastAPI dependencies are missing. Run 'python3 -m pip install -r ml-service/requirements.txt' first." >&2
  exit 1
fi

ML_SERVICE_HOST="${ML_SERVICE_HOST:-0.0.0.0}"
ML_SERVICE_PORT="${ML_SERVICE_PORT:-8000}"
echo "Starting MarketPulse ML service on $ML_SERVICE_HOST:$ML_SERVICE_PORT"
cd "$ML_DIR"
exec python3 -m uvicorn app.main:app --host "$ML_SERVICE_HOST" --port "$ML_SERVICE_PORT"
