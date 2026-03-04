#!/usr/bin/env bash

set -euo pipefail

INPUT_CSV=""
EXPECTED_SUCCESS="${EXPECTED_SUCCESS:-1000}"
EXPECTED_SOLD_OUT="${EXPECTED_SOLD_OUT:-500}"
EXPECTED_ALREADY="${EXPECTED_ALREADY:-0}"
MAX_P95_MS="${MAX_P95_MS:-2000}"
REPORT_DIR="${REPORT_DIR:-reports/loadtest/reports/claim-v2}"
STRICT=false

usage() {
  cat <<'USAGE'
Usage: scripts/aggregate_ticket_claim_results.sh --input <csv> [options]

Options:
  --input PATH              input csv from loadtest_ticket_claim.sh (required)
  --expected-success N      expected SUCCESS count (default: 1000)
  --expected-sold-out N     expected SOLD_OUT count (default: 500)
  --expected-already N      expected ALREADY count (default: 0)
  --max-p95-ms N            p95 threshold in milliseconds (default: 2000)
  --report-dir DIR          report output dir (default: reports/loadtest/reports/claim-v2)
  --strict                  exit with non-zero code when verdict is FAIL
  -h, --help                show help
USAGE
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --input)
        INPUT_CSV="$2"
        shift 2
        ;;
      --expected-success)
        EXPECTED_SUCCESS="$2"
        shift 2
        ;;
      --expected-sold-out)
        EXPECTED_SOLD_OUT="$2"
        shift 2
        ;;
      --expected-already)
        EXPECTED_ALREADY="$2"
        shift 2
        ;;
      --max-p95-ms)
        MAX_P95_MS="$2"
        shift 2
        ;;
      --report-dir)
        REPORT_DIR="$2"
        shift 2
        ;;
      --strict)
        STRICT=true
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "[aggregate] unknown argument: $1" >&2
        usage
        exit 1
        ;;
    esac
  done
}

validate() {
  [[ -n "$INPUT_CSV" ]] || { echo "[aggregate] --input is required" >&2; exit 1; }
  [[ -f "$INPUT_CSV" ]] || { echo "[aggregate] input not found: $INPUT_CSV" >&2; exit 1; }
  [[ "$EXPECTED_SUCCESS" =~ ^[0-9]+$ ]] || { echo "[aggregate] expected-success must be integer" >&2; exit 1; }
  [[ "$EXPECTED_SOLD_OUT" =~ ^[0-9]+$ ]] || { echo "[aggregate] expected-sold-out must be integer" >&2; exit 1; }
  [[ "$EXPECTED_ALREADY" =~ ^[0-9]+$ ]] || { echo "[aggregate] expected-already must be integer" >&2; exit 1; }
  [[ "$MAX_P95_MS" =~ ^[0-9]+(\.[0-9]+)?$ ]] || { echo "[aggregate] max-p95-ms must be numeric" >&2; exit 1; }
}

report_timestamp() {
  local name
  name="$(basename "$INPUT_CSV")"
  if [[ "$name" =~ ^claim-v2-([0-9]{8}-[0-9]{6})\.csv$ ]]; then
    echo "${BASH_REMATCH[1]}"
    return
  fi
  date +"%Y%m%d-%H%M%S"
}

percentile_value() {
  local sorted_file="$1"
  local sample_count="$2"
  local pct="$3"

  if (( sample_count == 0 )); then
    echo "0.000"
    return
  fi

  local rank
  rank="$(awk -v n="$sample_count" -v p="$pct" '
    BEGIN {
      raw = (p / 100.0) * n;
      r = int(raw);
      if (raw > r) r = r + 1;
      if (r < 1) r = 1;
      if (r > n) r = n;
      print r;
    }
  ')"

  awk -v line="$rank" 'NR==line {printf "%.3f", $1}' "$sorted_file"
}

main() {
  parse_args "$@"
  validate

  mkdir -p "$REPORT_DIR"

  local ts report_file
  ts="$(report_timestamp)"
  report_file="$REPORT_DIR/claim-v2-$ts.md"

  local total success sold_out already other_status
  local http_error curl_error request_error
  total="$(awk -F, 'NR>1 {c++} END {print c+0}' "$INPUT_CSV")"
  success="$(awk -F, 'NR>1 && $5=="SUCCESS" {c++} END {print c+0}' "$INPUT_CSV")"
  sold_out="$(awk -F, 'NR>1 && $5=="SOLD_OUT" {c++} END {print c+0}' "$INPUT_CSV")"
  already="$(awk -F, 'NR>1 && $5=="ALREADY" {c++} END {print c+0}' "$INPUT_CSV")"
  other_status="$(awk -F, 'NR>1 && $5!="SUCCESS" && $5!="SOLD_OUT" && $5!="ALREADY" {c++} END {print c+0}' "$INPUT_CSV")"

  http_error="$(awk -F, 'NR>1 && ($3<200 || $3>299) {c++} END {print c+0}' "$INPUT_CSV")"
  curl_error="$(awk -F, 'NR>1 && $7!="0" {c++} END {print c+0}' "$INPUT_CSV")"
  request_error="$(awk -F, 'NR>1 && ($7!="0" || $3<200 || $3>299) {c++} END {print c+0}' "$INPUT_CSV")"

  local diff_success diff_sold_out diff_already
  diff_success=$((success - EXPECTED_SUCCESS))
  diff_sold_out=$((sold_out - EXPECTED_SOLD_OUT))
  diff_already=$((already - EXPECTED_ALREADY))

  local http_error_rate request_error_rate
  if (( total > 0 )); then
    http_error_rate="$(awk -v e="$http_error" -v t="$total" 'BEGIN {printf "%.4f", (e/t)*100}')"
    request_error_rate="$(awk -v e="$request_error" -v t="$total" 'BEGIN {printf "%.4f", (e/t)*100}')"
  else
    http_error_rate="0.0000"
    request_error_rate="0.0000"
  fi

  local lat_tmp
  lat_tmp="$(mktemp)"
  trap "rm -f '$lat_tmp'" EXIT
  awk -F, 'NR>1 && $4 ~ /^[0-9]+(\.[0-9]+)?$/ {print $4}' "$INPUT_CSV" | sort -n > "$lat_tmp"
  local lat_count avg_ms p50_ms p95_ms p99_ms max_ms
  lat_count="$(wc -l < "$lat_tmp" | tr -d ' ')"
  avg_ms="$(awk -F, 'NR>1 && $4 ~ /^[0-9]+(\.[0-9]+)?$/ {sum+=$4; c++} END {if (c==0) printf "0.000"; else printf "%.3f", sum/c}' "$INPUT_CSV")"
  p50_ms="$(percentile_value "$lat_tmp" "$lat_count" 50)"
  p95_ms="$(percentile_value "$lat_tmp" "$lat_count" 95)"
  p99_ms="$(percentile_value "$lat_tmp" "$lat_count" 99)"
  max_ms="$(awk 'END {if (NR==0) printf "0.000"; else printf "%.3f", $1}' "$lat_tmp")"

  local expected_total
  expected_total=$((EXPECTED_SUCCESS + EXPECTED_SOLD_OUT + EXPECTED_ALREADY))

  local pass_counts pass_p95 pass_errors pass_total pass_other verdict
  pass_counts="FAIL"
  pass_p95="FAIL"
  pass_errors="FAIL"
  pass_total="FAIL"
  pass_other="FAIL"

  if (( diff_success == 0 && diff_sold_out == 0 && diff_already == 0 )); then
    pass_counts="PASS"
  fi
  if awk -v p95="$p95_ms" -v max="$MAX_P95_MS" 'BEGIN { exit !(p95 <= max) }'; then
    pass_p95="PASS"
  fi
  if (( request_error == 0 )); then
    pass_errors="PASS"
  fi
  if (( total == expected_total )); then
    pass_total="PASS"
  fi
  if (( other_status == 0 )); then
    pass_other="PASS"
  fi

  verdict="FAIL"
  if [[ "$pass_counts" == "PASS" && "$pass_p95" == "PASS" && "$pass_errors" == "PASS" && "$pass_total" == "PASS" && "$pass_other" == "PASS" ]]; then
    verdict="PASS"
  fi

  echo "input_csv=$INPUT_CSV"
  echo "total_requests=$total"
  echo "status_success=$success (expected=$EXPECTED_SUCCESS, diff=$diff_success)"
  echo "status_sold_out=$sold_out (expected=$EXPECTED_SOLD_OUT, diff=$diff_sold_out)"
  echo "status_already=$already (expected=$EXPECTED_ALREADY, diff=$diff_already)"
  echo "status_other=$other_status"
  echo "http_error_count=$http_error http_error_rate_pct=$http_error_rate"
  echo "curl_error_count=$curl_error"
  echo "request_error_count=$request_error request_error_rate_pct=$request_error_rate"
  echo "latency_ms avg=$avg_ms p50=$p50_ms p95=$p95_ms p99=$p99_ms max=$max_ms"
  echo "p95_threshold_ms=$MAX_P95_MS"
  echo "verdict=$verdict"
  echo "report_file=$report_file"

  cat > "$report_file" <<EOF
# Claim v2 Loadtest Report

- input_csv: $INPUT_CSV
- generated_at: $ts
- verdict: **$verdict**

## Scenario
- expected SUCCESS: $EXPECTED_SUCCESS
- expected SOLD_OUT: $EXPECTED_SOLD_OUT
- expected ALREADY: $EXPECTED_ALREADY
- expected total: $expected_total
- p95 threshold: ${MAX_P95_MS}ms

## Status Summary
| Metric | Actual | Expected | Diff |
|---|---:|---:|---:|
| SUCCESS | $success | $EXPECTED_SUCCESS | $diff_success |
| SOLD_OUT | $sold_out | $EXPECTED_SOLD_OUT | $diff_sold_out |
| ALREADY | $already | $EXPECTED_ALREADY | $diff_already |
| OTHER | $other_status | 0 | $other_status |
| TOTAL | $total | $expected_total | $((total - expected_total)) |

## Error Summary
| Metric | Value |
|---|---:|
| HTTP error count | $http_error |
| HTTP error rate (%) | $http_error_rate |
| Curl error count | $curl_error |
| Request error count | $request_error |
| Request error rate (%) | $request_error_rate |

## Latency (ms)
| Metric | Value |
|---|---:|
| avg | $avg_ms |
| p50 | $p50_ms |
| p95 | $p95_ms |
| p99 | $p99_ms |
| max | $max_ms |

## Gate Check
| Check | Result |
|---|---|
| status counts match expected | $pass_counts |
| p95 <= ${MAX_P95_MS}ms | $pass_p95 |
| request error rate == 0% | $pass_errors |
| total requests match expected | $pass_total |
| other status == 0 | $pass_other |
EOF

  if [[ "$STRICT" == "true" && "$verdict" != "PASS" ]]; then
    exit 1
  fi
}

main "$@"
