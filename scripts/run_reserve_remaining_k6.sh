#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EVENT_ID="${EVENT_ID:-1}"
USER_COUNT="${USER_COUNT:-2000}"
MAX_VUS="${MAX_VUS:-2000}"
RAMP_UP="${RAMP_UP:-5s}"
HOLD="${HOLD:-30s}"
RAMP_DOWN="${RAMP_DOWN:-5s}"
USER_PREFIX="${USER_PREFIX:-loadtest-}"
USER_PASSWORD="${USER_PASSWORD:-loadtest1234!}"
LOGIN_BATCH_SIZE="${LOGIN_BATCH_SIZE:-100}"
POLL_INTERVAL_SEC="${POLL_INTERVAL_SEC:-0.45}"
EXPECTED_TOTAL_REQUESTS="${EXPECTED_TOTAL_REQUESTS:-171250}"
REMAINING_PATH="${REMAINING_PATH:-/tickets/events}"
RESERVE_PATH="${RESERVE_PATH:-}"
OUTPUT_DIR="${OUTPUT_DIR:-reports/loadtest/raw}"
DRY_RUN=false

usage() {
  cat <<'USAGE'
Usage: scripts/run_reserve_remaining_k6.sh [options]

Options:
  --base-url URL                API base URL (default: http://localhost:8080)
  --event-id ID                 reserve event id (default: 1)
  --user-count N                number of login users (default: 2000)
  --max-vus N                   max virtual users (default: 2000)
  --ramp-up DURATION            ramp-up duration (default: 5s)
  --hold DURATION               hold duration (default: 30s)
  --ramp-down DURATION          ramp-down duration (default: 5s)
  --user-prefix PREFIX          studentId prefix (default: loadtest-)
  --user-password PASSWORD      loadtest user password (default: loadtest1234!)
  --login-batch-size N          setup login batch size (default: 100)
  --poll-interval-sec FLOAT     sleep seconds between polls (default: 0.45)
  --expected-total-requests N   expected total requests (default: 171250)
  --remaining-path PATH         remaining polling path (default: /tickets/events)
  --reserve-path PATH           reserve request path (default: /tickets/{eventId}/reserve)
  --output-dir DIR              summary output directory (default: reports/loadtest/raw)
  --dry-run                     print execution plan only
  -h, --help                    show help

Environment hints:
  For auto-seeding loadtest users at app startup:
    LOADTEST_USERS_ENABLED=true
    LOADTEST_USER_COUNT=2000
    LOADTEST_USER_PREFIX=loadtest-
    LOADTEST_USER_PASSWORD=loadtest1234!
USAGE
}

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[k6-run] missing command: $cmd" >&2
    exit 1
  fi
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
      --user-count)
        USER_COUNT="$2"
        shift 2
        ;;
      --max-vus)
        MAX_VUS="$2"
        shift 2
        ;;
      --ramp-up)
        RAMP_UP="$2"
        shift 2
        ;;
      --hold)
        HOLD="$2"
        shift 2
        ;;
      --ramp-down)
        RAMP_DOWN="$2"
        shift 2
        ;;
      --user-prefix)
        USER_PREFIX="$2"
        shift 2
        ;;
      --user-password)
        USER_PASSWORD="$2"
        shift 2
        ;;
      --login-batch-size)
        LOGIN_BATCH_SIZE="$2"
        shift 2
        ;;
      --poll-interval-sec)
        POLL_INTERVAL_SEC="$2"
        shift 2
        ;;
      --expected-total-requests)
        EXPECTED_TOTAL_REQUESTS="$2"
        shift 2
        ;;
      --remaining-path)
        REMAINING_PATH="$2"
        shift 2
        ;;
      --reserve-path)
        RESERVE_PATH="$2"
        shift 2
        ;;
      --output-dir)
        OUTPUT_DIR="$2"
        shift 2
        ;;
      --dry-run)
        DRY_RUN=true
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "[k6-run] unknown argument: $1" >&2
        usage
        exit 1
        ;;
    esac
  done
}

validate_numbers() {
  [[ "$USER_COUNT" =~ ^[0-9]+$ ]] || { echo "[k6-run] user-count must be integer" >&2; exit 1; }
  [[ "$MAX_VUS" =~ ^[0-9]+$ ]] || { echo "[k6-run] max-vus must be integer" >&2; exit 1; }
  [[ "$LOGIN_BATCH_SIZE" =~ ^[0-9]+$ ]] || { echo "[k6-run] login-batch-size must be integer" >&2; exit 1; }
  [[ "$EXPECTED_TOTAL_REQUESTS" =~ ^[0-9]+$ ]] || { echo "[k6-run] expected-total-requests must be integer" >&2; exit 1; }
  [[ "$POLL_INTERVAL_SEC" =~ ^[0-9]+(\.[0-9]+)?$ ]] || { echo "[k6-run] poll-interval-sec must be numeric" >&2; exit 1; }

  (( USER_COUNT > 0 )) || { echo "[k6-run] user-count must be > 0" >&2; exit 1; }
  (( MAX_VUS > 0 )) || { echo "[k6-run] max-vus must be > 0" >&2; exit 1; }
  (( LOGIN_BATCH_SIZE > 0 )) || { echo "[k6-run] login-batch-size must be > 0" >&2; exit 1; }
}

ensure_api_ready() {
  local http_code
  http_code="$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/tickets/events" || true)"
  if [[ "$http_code" != "200" ]]; then
    echo "[k6-run] API is not ready: GET $BASE_URL/tickets/events -> $http_code" >&2
    exit 1
  fi
}

student_id_by_seq() {
  local seq="$1"
  printf "%s%06d" "$USER_PREFIX" "$seq"
}

login_check() {
  local student_id="$1"
  local tmp_file
  tmp_file="$(mktemp)"
  trap "rm -f '$tmp_file'" RETURN

  local http_code
  http_code="$(curl -s -o "$tmp_file" -w "%{http_code}" \
    -X POST "$BASE_URL/user/login" \
    -H "Content-Type: application/json" \
    -d "{\"studentId\":\"$student_id\",\"password\":\"$USER_PASSWORD\"}" || true)"

  if [[ "$http_code" != "200" ]]; then
    echo "[k6-run] login precheck failed for studentId=$student_id (http=$http_code)" >&2
    echo "[k6-run] body=$(tr '\n' ' ' < "$tmp_file")" >&2
    echo "[k6-run] loadtest users may not be seeded. Restart app with:" >&2
    echo "  LOADTEST_USERS_ENABLED=true LOADTEST_USER_COUNT=$USER_COUNT LOADTEST_USER_PREFIX=$USER_PREFIX LOADTEST_USER_PASSWORD=$USER_PASSWORD" >&2
    exit 1
  fi
}

main() {
  parse_args "$@"

  require_cmd curl
  require_cmd date
  require_cmd tee
  require_cmd mktemp
  require_cmd tr
  validate_numbers

  if [[ -z "$RESERVE_PATH" ]]; then
    RESERVE_PATH="/tickets/$EVENT_ID/reserve"
  fi

  mkdir -p "$OUTPUT_DIR"

  local ts
  ts="$(date +"%Y%m%d-%H%M%S")"
  local summary_json="$OUTPUT_DIR/reserve-remaining-k6-$ts-summary.json"
  local summary_export_json="$OUTPUT_DIR/reserve-remaining-k6-$ts-k6-summary.json"
  local run_log="$OUTPUT_DIR/reserve-remaining-k6-$ts.log"

  echo "[k6-run] base_url=$BASE_URL event_id=$EVENT_ID user_count=$USER_COUNT max_vus=$MAX_VUS"
  echo "[k6-run] stages=$RAMP_UP/$HOLD/$RAMP_DOWN poll_interval_sec=$POLL_INTERVAL_SEC"
  echo "[k6-run] reserve_path=$RESERVE_PATH remaining_path=$REMAINING_PATH"
  echo "[k6-run] summary_json=$summary_json"
  echo "[k6-run] k6_summary_export=$summary_export_json"
  echo "[k6-run] run_log=$run_log"

  if [[ "$DRY_RUN" == "true" ]]; then
    echo "[k6-run] dry-run completed"
    exit 0
  fi

  require_cmd k6

  ensure_api_ready

  local first_sid last_sid
  first_sid="$(student_id_by_seq 1)"
  last_sid="$(student_id_by_seq "$USER_COUNT")"

  echo "[k6-run] login precheck first user: $first_sid"
  login_check "$first_sid"
  echo "[k6-run] login precheck last user: $last_sid"
  login_check "$last_sid"

  echo "[k6-run] start k6"
  k6 run \
    --summary-export "$summary_export_json" \
    -e BASE_URL="$BASE_URL" \
    -e EVENT_ID="$EVENT_ID" \
    -e USER_COUNT="$USER_COUNT" \
    -e MAX_VUS="$MAX_VUS" \
    -e RAMP_UP="$RAMP_UP" \
    -e HOLD="$HOLD" \
    -e RAMP_DOWN="$RAMP_DOWN" \
    -e USER_PREFIX="$USER_PREFIX" \
    -e USER_PASSWORD="$USER_PASSWORD" \
    -e LOGIN_BATCH_SIZE="$LOGIN_BATCH_SIZE" \
    -e POLL_INTERVAL_SEC="$POLL_INTERVAL_SEC" \
    -e EXPECTED_TOTAL_REQUESTS="$EXPECTED_TOTAL_REQUESTS" \
    -e REMAINING_PATH="$REMAINING_PATH" \
    -e RESERVE_PATH="$RESERVE_PATH" \
    -e SUMMARY_PATH="$summary_json" \
    scripts/k6/reserve_remaining_40s.js | tee "$run_log"

  echo "[k6-run] done"
}

main "$@"
