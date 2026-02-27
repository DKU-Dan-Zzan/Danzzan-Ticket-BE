#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
AGG_SCRIPT="$ROOT_DIR/scripts/aggregate_ticket_claim_results.sh"

assert_contains() {
  local file="$1"
  local pattern="$2"
  if ! grep -q "$pattern" "$file"; then
    echo "[test] expected pattern not found: $pattern" >&2
    echo "[test] in file: $file" >&2
    exit 1
  fi
}

main() {
  if [[ ! -x "$AGG_SCRIPT" ]]; then
    echo "[test] aggregate script is not executable: $AGG_SCRIPT" >&2
    exit 1
  fi

  local tmp_dir
  tmp_dir="$(mktemp -d)"
  trap "rm -rf '$tmp_dir'" EXIT

  local csv_pass csv_fail_counts csv_fail_p95
  local out_pass out_fail_counts out_fail_p95
  csv_pass="$tmp_dir/pass.csv"
  csv_fail_counts="$tmp_dir/fail-counts.csv"
  csv_fail_p95="$tmp_dir/fail-p95.csv"
  out_pass="$tmp_dir/pass.out"
  out_fail_counts="$tmp_dir/fail-counts.out"
  out_fail_p95="$tmp_dir/fail-p95.out"

  cat > "$csv_pass" <<'EOF'
request_id,user_id,http_code,time_total_ms,status,remaining,curl_exit,error_type
1,u-1,200,10.000,SUCCESS,1,0,NONE
2,u-2,200,20.000,SUCCESS,0,0,NONE
3,u-3,200,30.000,SOLD_OUT,,0,NONE
EOF

  "$AGG_SCRIPT" \
    --input "$csv_pass" \
    --expected-success 2 \
    --expected-sold-out 1 \
    --expected-already 0 \
    --max-p95-ms 100 \
    --report-dir "$tmp_dir/reports-pass" \
    --strict > "$out_pass"

  assert_contains "$out_pass" '^verdict=PASS$'
  assert_contains "$out_pass" '^status_other=0$'
  assert_contains "$out_pass" '^request_error_count=0 request_error_rate_pct=0.0000$'

  cat > "$csv_fail_counts" <<'EOF'
request_id,user_id,http_code,time_total_ms,status,remaining,curl_exit,error_type
1,u-1,200,10.000,SUCCESS,1,0,NONE
2,u-2,200,20.000,SOLD_OUT,,0,NONE
3,u-3,200,30.000,ALREADY,,0,NONE
EOF

  set +e
  "$AGG_SCRIPT" \
    --input "$csv_fail_counts" \
    --expected-success 2 \
    --expected-sold-out 1 \
    --expected-already 0 \
    --max-p95-ms 100 \
    --report-dir "$tmp_dir/reports-fail-counts" \
    --strict > "$out_fail_counts"
  local rc_counts=$?
  set -e

  if [[ "$rc_counts" -eq 0 ]]; then
    echo "[test] expected non-zero exit for strict fail(count mismatch)" >&2
    exit 1
  fi
  assert_contains "$out_fail_counts" '^verdict=FAIL$'
  assert_contains "$out_fail_counts" '^status_already=1 (expected=0, diff=1)$'

  cat > "$csv_fail_p95" <<'EOF'
request_id,user_id,http_code,time_total_ms,status,remaining,curl_exit,error_type
1,u-1,200,10.000,SUCCESS,1,0,NONE
2,u-2,200,20.000,SUCCESS,0,0,NONE
3,u-3,200,3000.000,SOLD_OUT,,0,NONE
EOF

  set +e
  "$AGG_SCRIPT" \
    --input "$csv_fail_p95" \
    --expected-success 2 \
    --expected-sold-out 1 \
    --expected-already 0 \
    --max-p95-ms 100 \
    --report-dir "$tmp_dir/reports-fail-p95" \
    --strict > "$out_fail_p95"
  local rc_p95=$?
  set -e

  if [[ "$rc_p95" -eq 0 ]]; then
    echo "[test] expected non-zero exit for strict fail(p95)" >&2
    exit 1
  fi
  assert_contains "$out_fail_p95" '^verdict=FAIL$'
  assert_contains "$out_fail_p95" '^p95_threshold_ms=100$'

  echo "[test] aggregate regression tests passed"
}

main "$@"
