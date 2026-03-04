#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EVENT_ID="${EVENT_ID:-festival-day1}"
STOCK="${STOCK:-1000}"
USERS="${USERS:-1500}"
CONCURRENCY="${CONCURRENCY:-1500}"
TIMEOUT_SEC="${TIMEOUT_SEC:-5}"
MAX_P95_MS="${MAX_P95_MS:-2000}"
OUTPUT_DIR="${OUTPUT_DIR:-reports/loadtest/raw}"
REPORT_DIR="${REPORT_DIR:-reports/loadtest}"
ADMIN_STUDENT_ID="${ADMIN_STUDENT_ID:-1234}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-1234}"

usage() {
  cat <<'USAGE'
Usage: scripts/run_claim_v2_e2e.sh [options]

Options:
  --base-url URL            API base URL (default: http://localhost:8080)
  --event-id ID             eventId (default: festival-day1)
  --stock N                 init stock (default: 1000)
  --users N                 request/user count (default: 1500)
  --concurrency N           parallel requests (default: 1500)
  --timeout-sec N           curl timeout sec (default: 5)
  --max-p95-ms N            p95 threshold ms (default: 2000)
  --output-dir DIR          loadtest raw output dir (default: reports/loadtest/raw)
  --report-dir DIR          aggregate report dir (default: reports/loadtest)
  --admin-student-id ID     admin login id (default: 1234)
  --admin-password PW       admin login password (default: 1234)
  -h, --help                show help
USAGE
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --base-url)
        BASE_URL="$2"
        shift 2
        ;;
      --event-id)
        EVENT_ID="$2"
        shift 2
        ;;
      --stock)
        STOCK="$2"
        shift 2
        ;;
      --users)
        USERS="$2"
        shift 2
        ;;
      --concurrency)
        CONCURRENCY="$2"
        shift 2
        ;;
      --timeout-sec)
        TIMEOUT_SEC="$2"
        shift 2
        ;;
      --max-p95-ms)
        MAX_P95_MS="$2"
        shift 2
        ;;
      --output-dir)
        OUTPUT_DIR="$2"
        shift 2
        ;;
      --report-dir)
        REPORT_DIR="$2"
        shift 2
        ;;
      --admin-student-id)
        ADMIN_STUDENT_ID="$2"
        shift 2
        ;;
      --admin-password)
        ADMIN_PASSWORD="$2"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "[e2e] unknown argument: $1" >&2
        usage
        exit 1
        ;;
    esac
  done
}

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[e2e] missing command: $cmd" >&2
    exit 1
  fi
}

validate_numbers() {
  [[ "$STOCK" =~ ^[0-9]+$ ]] || { echo "[e2e] stock must be integer" >&2; exit 1; }
  [[ "$USERS" =~ ^[0-9]+$ ]] || { echo "[e2e] users must be integer" >&2; exit 1; }
  [[ "$CONCURRENCY" =~ ^[0-9]+$ ]] || { echo "[e2e] concurrency must be integer" >&2; exit 1; }
  [[ "$TIMEOUT_SEC" =~ ^[0-9]+$ ]] || { echo "[e2e] timeout-sec must be integer" >&2; exit 1; }
  [[ "$MAX_P95_MS" =~ ^[0-9]+(\.[0-9]+)?$ ]] || { echo "[e2e] max-p95-ms must be numeric" >&2; exit 1; }
}

assert_api_available() {
  local http_code
  http_code="$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout "$TIMEOUT_SEC" --max-time "$TIMEOUT_SEC" "$BASE_URL/tickets/events" || true)"
  if [[ "$http_code" != "200" ]]; then
    echo "[e2e] API is not ready at $BASE_URL (GET /tickets/events -> $http_code)" >&2
    exit 1
  fi
}

admin_login() {
  local login_file="$1"
  local http_code
  http_code="$(curl -s -o "$login_file" -w "%{http_code}" \
    --connect-timeout "$TIMEOUT_SEC" --max-time "$TIMEOUT_SEC" \
    -X POST "$BASE_URL/user/login" \
    -H "Content-Type: application/json" \
    -d "{\"studentId\":\"$ADMIN_STUDENT_ID\",\"password\":\"$ADMIN_PASSWORD\"}")"
  if [[ "$http_code" != "200" ]]; then
    echo "[e2e] admin login failed (http=$http_code)" >&2
    echo "[e2e] body=$(tr '\n' ' ' < "$login_file")" >&2
    exit 1
  fi
}

extract_access_token() {
  local login_file="$1"
  sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' "$login_file" | head -n1
}

latest_csv_path() {
  ls -t "$OUTPUT_DIR"/claim-v2-*.csv 2>/dev/null | head -n1 || true
}

main() {
  parse_args "$@"

  require_cmd curl
  require_cmd sed
  require_cmd ls
  require_cmd tr
  require_cmd mktemp
  validate_numbers

  mkdir -p "$OUTPUT_DIR" "$REPORT_DIR"
  assert_api_available

  local login_tmp agg_tmp
  login_tmp="$(mktemp)"
  agg_tmp="$(mktemp)"
  trap "rm -f '$login_tmp' '$agg_tmp'" EXIT

  echo "[e2e] login: $ADMIN_STUDENT_ID"
  admin_login "$login_tmp"
  local admin_token
  admin_token="$(extract_access_token "$login_tmp")"
  if [[ -z "$admin_token" ]]; then
    echo "[e2e] could not parse accessToken from /user/login response" >&2
    exit 1
  fi

  echo "[e2e] run loadtest with init (stock=$STOCK users=$USERS concurrency=$CONCURRENCY)"
  ADMIN_TOKEN="$admin_token" scripts/loadtest_ticket_claim.sh \
    --init \
    --base-url "$BASE_URL" \
    --event-id "$EVENT_ID" \
    --stock "$STOCK" \
    --users "$USERS" \
    --concurrency "$CONCURRENCY" \
    --timeout-sec "$TIMEOUT_SEC" \
    --output-dir "$OUTPUT_DIR"

  local csv
  csv="$(latest_csv_path)"
  if [[ -z "$csv" ]]; then
    echo "[e2e] no csv output found in $OUTPUT_DIR" >&2
    exit 1
  fi

  local expected_success expected_sold_out
  expected_success="$STOCK"
  expected_sold_out="$((USERS - STOCK))"
  if (( STOCK > USERS )); then
    expected_success="$USERS"
    expected_sold_out="0"
  fi

  echo "[e2e] aggregate and gate check (--strict)"
  scripts/aggregate_ticket_claim_results.sh \
    --input "$csv" \
    --expected-success "$expected_success" \
    --expected-sold-out "$expected_sold_out" \
    --expected-already 0 \
    --max-p95-ms "$MAX_P95_MS" \
    --report-dir "$REPORT_DIR" \
    --strict | tee "$agg_tmp"

  local verdict report_file
  verdict="$(sed -n 's/^verdict=//p' "$agg_tmp" | tail -n1)"
  report_file="$(sed -n 's/^report_file=//p' "$agg_tmp" | tail -n1)"

  echo "[e2e] done verdict=$verdict"
  echo "[e2e] csv=$csv"
  echo "[e2e] report=$report_file"
}

main "$@"
