#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "$ROOT_DIR/backend/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/backend/.env"
  set +a
fi

required_variables=(SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD)
for variable in "${required_variables[@]}"; do
  if [[ -z "${!variable:-}" ]]; then
    echo "Error: $variable is required. Copy backend/.env.example to backend/.env and set it." >&2
    exit 1
  fi
done

if ! command -v java >/dev/null 2>&1; then
  echo "Error: Java 21+ is required to start the backend." >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Error: Maven 3.9+ is required to start the backend." >&2
  exit 1
fi

echo "Starting MarketPulse backend on port ${SERVER_PORT:-8080}"
cd "$ROOT_DIR"
exec mvn -f backend/pom.xml spring-boot:run
