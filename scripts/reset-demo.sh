#!/usr/bin/env bash
# Deterministic demo reset — requires local/demo/test profile (never production).
set -euo pipefail
API_URL="${API_URL:-http://127.0.0.1:8080}"
echo "POST ${API_URL}/api/dev/reset"
curl -fsS -X POST "${API_URL}/api/dev/reset"
echo
