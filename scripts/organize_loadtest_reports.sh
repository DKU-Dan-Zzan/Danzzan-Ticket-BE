#!/usr/bin/env bash

set -euo pipefail

REPORTS_ROOT="${REPORTS_ROOT:-reports/loadtest}"
SCENARIO="${SCENARIO:-claim-v2}"
REPORTS_DIR="${REPORTS_ROOT}/reports/${SCENARIO}"
ARCHIVE_DIR="${REPORTS_ROOT}/archive/${SCENARIO}"
ARCHIVE_OLD_PASS=false
MIGRATE_LEGACY=true
REPORTS_DIR_EXPLICIT=false
ARCHIVE_DIR_EXPLICIT=false

refresh_dirs() {
  if [[ "$REPORTS_DIR_EXPLICIT" == "false" ]]; then
    REPORTS_DIR="${REPORTS_ROOT}/reports/${SCENARIO}"
  fi
  if [[ "$ARCHIVE_DIR_EXPLICIT" == "false" ]]; then
    ARCHIVE_DIR="${REPORTS_ROOT}/archive/${SCENARIO}"
  fi
}

usage() {
  cat <<'USAGE'
Usage: scripts/organize_loadtest_reports.sh [options]

Options:
  --reports-root DIR        loadtest root directory (default: reports/loadtest)
  --scenario NAME           report scenario prefix (default: claim-v2)
  --reports-dir DIR         report directory (default: reports/loadtest/reports/<scenario>)
  --archive-dir DIR         archive directory (default: reports/loadtest/archive/<scenario>)
  --archive-old-pass        archive all PASS reports except latest PASS
  --no-migrate-legacy       skip migration from legacy flat directories
  -h, --help                show help

Rules:
  - Latest PASS report in reports dir is the source of truth.
  - FAIL reports are moved to archive dir.
  - PASS reports are kept by default (except when --archive-old-pass is used).
  - Legacy migration (enabled by default):
    - reports/loadtest/<scenario>-*.md -> reports/loadtest/reports/<scenario>/
    - reports/loadtest/archive/<scenario>-*.md -> reports/loadtest/archive/<scenario>/
USAGE
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --reports-root)
        REPORTS_ROOT="$2"
        shift 2
        refresh_dirs
        ;;
      --scenario)
        SCENARIO="$2"
        shift 2
        refresh_dirs
        ;;
      --reports-dir)
        REPORTS_DIR="$2"
        REPORTS_DIR_EXPLICIT=true
        shift 2
        ;;
      --archive-dir)
        ARCHIVE_DIR="$2"
        ARCHIVE_DIR_EXPLICIT=true
        shift 2
        ;;
      --archive-old-pass)
        ARCHIVE_OLD_PASS=true
        shift
        ;;
      --no-migrate-legacy)
        MIGRATE_LEGACY=false
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

move_pattern() {
  local pattern="$1"
  local target_dir="$2"
  local moved=0
  local file

  shopt -s nullglob
  for file in $pattern; do
    [[ -f "$file" ]] || continue
    mv "$file" "$target_dir/"
    moved=$((moved + 1))
  done
  shopt -u nullglob

  echo "$moved"
}

move_legacy_reports() {
  if [[ "$MIGRATE_LEGACY" != "true" ]]; then
    echo "0"
    return
  fi

  local moved_root moved_archive
  moved_root="$(move_pattern "$REPORTS_ROOT/$SCENARIO-*.md" "$REPORTS_DIR")"
  moved_archive="$(move_pattern "$REPORTS_ROOT/archive/$SCENARIO-*.md" "$ARCHIVE_DIR")"
  echo $((moved_root + moved_archive))
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
  for report_file in "$REPORTS_DIR"/"$SCENARIO"-*.md; do
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
  mkdir -p "$REPORTS_DIR" "$ARCHIVE_DIR"

  local moved_legacy
  moved_legacy="$(move_legacy_reports)"

  local latest_pass
  latest_pass="$(latest_pass_report)"

  local report_file verdict moved_fail moved_old_pass kept_pass unknown_count
  moved_fail=0
  moved_old_pass=0
  kept_pass=0
  unknown_count=0

  for report_file in "$REPORTS_DIR"/"$SCENARIO"-*.md; do
    [[ -f "$report_file" ]] || continue

    verdict="$(report_verdict "$report_file")"
    case "$verdict" in
      FAIL)
        mv "$report_file" "$ARCHIVE_DIR/"
        moved_fail=$((moved_fail + 1))
        ;;
      PASS)
        if [[ "$ARCHIVE_OLD_PASS" == "true" && "$report_file" != "$latest_pass" ]]; then
          mv "$report_file" "$ARCHIVE_DIR/"
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

  echo "reports_root=$REPORTS_ROOT"
  echo "scenario=$SCENARIO"
  echo "reports_dir=$REPORTS_DIR"
  echo "archive_dir=$ARCHIVE_DIR"
  echo "moved_legacy=$moved_legacy"
  echo "latest_pass=${latest_pass:-NONE}"
  echo "moved_fail=$moved_fail"
  echo "moved_old_pass=$moved_old_pass"
  echo "kept_pass=$kept_pass"
  echo "unknown_reports=$unknown_count"
}

main "$@"
