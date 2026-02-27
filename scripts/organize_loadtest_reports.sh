#!/usr/bin/env bash

set -euo pipefail

REPORTS_DIR="${REPORTS_DIR:-reports/loadtest}"
ARCHIVE_OLD_PASS=false

usage() {
  cat <<'USAGE'
Usage: scripts/organize_loadtest_reports.sh [options]

Options:
  --reports-dir DIR         report directory (default: reports/loadtest)
  --archive-old-pass        archive all PASS reports except latest PASS
  -h, --help                show help

Rules:
  - Latest PASS report in reports dir is the source of truth.
  - FAIL reports are moved to reports/archive.
  - PASS reports are kept by default (except when --archive-old-pass is used).
USAGE
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --reports-dir)
        REPORTS_DIR="$2"
        shift 2
        ;;
      --archive-old-pass)
        ARCHIVE_OLD_PASS=true
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "[organize] unknown argument: $1" >&2
        usage
        exit 1
        ;;
    esac
  done
}

report_verdict() {
  local report_file="$1"
  if grep -q "verdict: \*\*PASS\*\*" "$report_file"; then
    echo "PASS"
    return
  fi
  if grep -q "verdict: \*\*FAIL\*\*" "$report_file"; then
    echo "FAIL"
    return
  fi
  echo "UNKNOWN"
}

latest_pass_report() {
  local report_file verdict
  local latest=""
  for report_file in "$REPORTS_DIR"/claim-v2-*.md; do
    [[ -f "$report_file" ]] || continue
    verdict="$(report_verdict "$report_file")"
    if [[ "$verdict" == "PASS" ]]; then
      latest="$report_file"
    fi
  done
  echo "$latest"
}

main() {
  parse_args "$@"
  local archive_dir="$REPORTS_DIR/archive"
  mkdir -p "$archive_dir"

  local latest_pass
  latest_pass="$(latest_pass_report)"

  local report_file verdict moved_fail moved_old_pass kept_pass unknown_count
  moved_fail=0
  moved_old_pass=0
  kept_pass=0
  unknown_count=0

  for report_file in "$REPORTS_DIR"/claim-v2-*.md; do
    [[ -f "$report_file" ]] || continue

    verdict="$(report_verdict "$report_file")"
    case "$verdict" in
      FAIL)
        mv "$report_file" "$archive_dir/"
        moved_fail=$((moved_fail + 1))
        ;;
      PASS)
        if [[ "$ARCHIVE_OLD_PASS" == "true" && "$report_file" != "$latest_pass" ]]; then
          mv "$report_file" "$archive_dir/"
          moved_old_pass=$((moved_old_pass + 1))
        else
          kept_pass=$((kept_pass + 1))
        fi
        ;;
      *)
        unknown_count=$((unknown_count + 1))
        ;;
    esac
  done

  echo "reports_dir=$REPORTS_DIR"
  echo "archive_dir=$archive_dir"
  echo "latest_pass=${latest_pass:-NONE}"
  echo "moved_fail=$moved_fail"
  echo "moved_old_pass=$moved_old_pass"
  echo "kept_pass=$kept_pass"
  echo "unknown_reports=$unknown_count"
}

main "$@"
