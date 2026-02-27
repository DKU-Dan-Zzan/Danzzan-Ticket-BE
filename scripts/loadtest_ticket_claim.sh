#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EVENT_ID="${EVENT_ID:-festival-day1}"
STOCK="${STOCK:-1000}"
USERS="${USERS:-1500}"
CONCURRENCY="${CONCURRENCY:-200}"
TIMEOUT_SEC="${TIMEOUT_SEC:-5}"
OUTPUT_DIR="${OUTPUT_DIR:-reports/loadtest/raw}"
INIT_ENABLED=false
DRY_RUN=false

usage() {
  cat <<'USAGE'
Usage: scripts/loadtest_ticket_claim.sh [options]

Options:
  --base-url URL         API base URL (default: http://localhost:8080)
  --event-id ID          eventId for claim scenario (default: festival-day1)
  --stock N              stock for init API (default: 1000)
  --users N              number of requests/users (default: 1500)
  --concurrency N        parallel workers (default: 200)
  --timeout-sec N        per-request timeout sec for curl (default: 5)
  --output-dir DIR       raw output directory (default: reports/loadtest/raw)
  --init                 call admin init API before load
  --dry-run              validate parameters and print execution plan only
  -h, --help             show help

Environment:
  ADMIN_TOKEN            required when --init is used
USAGE
}

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[loadtest] missing command: $cmd" >&2
    exit 1
  fi
}

json_field() {
  local file="$1"
  local key="$2"
  sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\{0,1\}\([^\",}]*\)\"\{0,1\}.*/\1/p" "$file" | head -n1
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
      --output-dir)
        OUTPUT_DIR="$2"
        shift 2
        ;;
      --init)
        INIT_ENABLED=true
        shift
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
        echo "[loadtest] unknown argument: $1" >&2
        usage
        exit 1
        ;;
    esac
  done
}

validate_numbers() {
  [[ "$STOCK" =~ ^[0-9]+$ ]] || { echo "[loadtest] stock must be integer" >&2; exit 1; }
  [[ "$USERS" =~ ^[0-9]+$ ]] || { echo "[loadtest] users must be integer" >&2; exit 1; }
  [[ "$CONCURRENCY" =~ ^[0-9]+$ ]] || { echo "[loadtest] concurrency must be integer" >&2; exit 1; }
  [[ "$TIMEOUT_SEC" =~ ^[0-9]+$ ]] || { echo "[loadtest] timeout-sec must be integer" >&2; exit 1; }
  (( USERS > 0 )) || { echo "[loadtest] users must be > 0" >&2; exit 1; }
  (( CONCURRENCY > 0 )) || { echo "[loadtest] concurrency must be > 0" >&2; exit 1; }
  (( TIMEOUT_SEC > 0 )) || { echo "[loadtest] timeout-sec must be > 0" >&2; exit 1; }
}

do_init() {
  if [[ -z "${ADMIN_TOKEN:-}" ]]; then
    echo "[loadtest] --init requires ADMIN_TOKEN environment variable" >&2
    exit 1
  fi

  local init_file="$1"
  local init_http
  init_http="$(curl -sS -o "$init_file" -w "%{http_code}" \
    --connect-timeout "$TIMEOUT_SEC" --max-time "$TIMEOUT_SEC" \
    -X POST "$BASE_URL/api/admin/ticket/init" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"eventId\":\"$EVENT_ID\",\"stock\":$STOCK}")"

  if [[ "$init_http" != "200" ]]; then
    echo "[loadtest] init failed http=$init_http body=$(tr '\n' ' ' < "$init_file")" >&2
    exit 1
  fi

  local success
  success="$(json_field "$init_file" "success")"
  if [[ "$success" != "true" ]]; then
    echo "[loadtest] init response indicates failure: $(tr '\n' ' ' < "$init_file")" >&2
    exit 1
  fi
}

request_once() {
  local idx="$1"
  local user_id="$2"
  local tmp_dir="$TMP_DIR"
  local body_file="$tmp_dir/body-${idx}.json"
  local out_file="$tmp_dir/line-${idx}.csv"
  local payload
  payload="{\"eventId\":\"$EVENT_ID\",\"userId\":\"$user_id\"}"

  set +e
  local curl_result
  curl_result="$(curl -s -o "$body_file" -w "%{http_code},%{time_total}" \
    --connect-timeout "$TIMEOUT_SEC" --max-time "$TIMEOUT_SEC" \
    -X POST "$BASE_URL/tickets/request" \
    -H "Content-Type: application/json" \
    -d "$payload")"
  local rc=$?
  set -e

  local http_code="000"
  local time_total="0"
  if [[ "$curl_result" == *,* ]]; then
    http_code="${curl_result%%,*}"
    time_total="${curl_result##*,}"
  fi

  local ms
  ms="$(awk -v t="$time_total" 'BEGIN { printf "%.3f", t * 1000 }')"

  local status="CURL_ERROR"
  local remaining=""
  local error_type="NONE"
  if [[ "$rc" -eq 0 ]]; then
    status="$(json_field "$body_file" "status")"
    remaining="$(json_field "$body_file" "remaining")"
    if [[ -z "$status" ]]; then
      status="PARSE_ERROR"
    fi
    if [[ "$http_code" -lt 200 || "$http_code" -gt 299 ]]; then
      error_type="HTTP_ERROR"
    fi
  else
    error_type="CURL_ERROR"
  fi

  printf "%s,%s,%s,%s,%s,%s,%s,%s\n" \
    "$idx" "$user_id" "$http_code" "$ms" "$status" "$remaining" "$rc" "$error_type" > "$out_file"
}

main() {
  parse_args "$@"
  validate_numbers
  require_cmd curl
  require_cmd awk
  require_cmd xargs
  require_cmd sed
  require_cmd tr
  require_cmd date

  mkdir -p "$OUTPUT_DIR"
  local ts
  ts="$(date +"%Y%m%d-%H%M%S")"
  local csv_file="$OUTPUT_DIR/claim-v2-$ts.csv"
  local meta_file="$OUTPUT_DIR/claim-v2-$ts.json"

  echo "[loadtest] base_url=$BASE_URL event_id=$EVENT_ID stock=$STOCK users=$USERS concurrency=$CONCURRENCY init=$INIT_ENABLED"
  echo "[loadtest] output_csv=$csv_file"
  echo "[loadtest] output_meta=$meta_file"

  if [[ "$DRY_RUN" == "true" ]]; then
    cat > "$meta_file" <<EOF
{
  "mode": "dry-run",
  "timestamp": "$ts",
  "baseUrl": "$BASE_URL",
  "eventId": "$EVENT_ID",
  "stock": $STOCK,
  "users": $USERS,
  "concurrency": $CONCURRENCY,
  "outputCsv": "$csv_file"
}
EOF
    echo "[loadtest] dry-run completed"
    exit 0
  fi

  local tmp_dir="$OUTPUT_DIR/tmp-$ts"
  mkdir -p "$tmp_dir"

  if [[ "$INIT_ENABLED" == "true" ]]; then
    echo "[loadtest] calling init endpoint"
    do_init "$tmp_dir/init-response.json"
  fi

  local user_file="$tmp_dir/users.txt"
  seq 1 "$USERS" | awk '{ printf "%d loadtest-user-%06d\n", $1, $1 }' > "$user_file"

  export BASE_URL EVENT_ID TIMEOUT_SEC TMP_DIR="$tmp_dir"
  export -f json_field request_once

  echo "[loadtest] sending requests"
  xargs -P "$CONCURRENCY" -n2 bash -c 'request_once "$@"' _ < "$user_file"

  {
    echo "request_id,user_id,http_code,time_total_ms,status,remaining,curl_exit,error_type"
    cat "$tmp_dir"/line-*.csv | sort -t, -k1,1n
  } > "$csv_file"

  local total_requests
  local status_success
  local status_sold_out
  local status_already
  local status_other
  local error_rows
  total_requests="$(awk -F, 'NR>1 {c++} END {print c+0}' "$csv_file")"
  status_success="$(awk -F, 'NR>1 && $5=="SUCCESS" {c++} END {print c+0}' "$csv_file")"
  status_sold_out="$(awk -F, 'NR>1 && $5=="SOLD_OUT" {c++} END {print c+0}' "$csv_file")"
  status_already="$(awk -F, 'NR>1 && $5=="ALREADY" {c++} END {print c+0}' "$csv_file")"
  status_other="$(awk -F, 'NR>1 && $5!="SUCCESS" && $5!="SOLD_OUT" && $5!="ALREADY" {c++} END {print c+0}' "$csv_file")"
  error_rows="$(awk -F, 'NR>1 && ($7!="0" || $3<200 || $3>299) {c++} END {print c+0}' "$csv_file")"

  cat > "$meta_file" <<EOF
{
  "mode": "run",
  "timestamp": "$ts",
  "baseUrl": "$BASE_URL",
  "eventId": "$EVENT_ID",
  "stock": $STOCK,
  "users": $USERS,
  "concurrency": $CONCURRENCY,
  "outputCsv": "$csv_file",
  "summary": {
    "totalRequests": $total_requests,
    "success": $status_success,
    "soldOut": $status_sold_out,
    "already": $status_already,
    "other": $status_other,
    "errorRows": $error_rows
  }
}
EOF

  rm -rf "$tmp_dir"
  echo "[loadtest] completed: $csv_file"
}

main "$@"
