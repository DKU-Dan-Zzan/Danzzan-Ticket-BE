#!/usr/bin/env bash

set -euo pipefail

RAW_ROOT="${RAW_ROOT:-reports/loadtest/raw}"
CLAIM_DIR="$RAW_ROOT/claim-v2"
K6_RESERVE_DIR="$RAW_ROOT/k6/reserve-remaining"
DRY_RUN=false

usage() {
  cat <<'USAGE'
Usage: scripts/organize_loadtest_raw_artifacts.sh [options]

Options:
  --raw-root DIR      raw artifact root (default: reports/loadtest/raw)
  --dry-run           print move plan only
  -h, --help          show help

Organize rules:
- claim-v2-*.{csv,json,log} -> <raw-root>/claim-v2/
- reserve-remaining-k6-*     -> <raw-root>/k6/reserve-remaining/
USAGE
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --raw-root)
        RAW_ROOT="$2"
        CLAIM_DIR="$RAW_ROOT/claim-v2"
        K6_RESERVE_DIR="$RAW_ROOT/k6/reserve-remaining"
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
        echo "[organize-raw] unknown argument: $1" >&2
        usage
        exit 1
        ;;
    esac
  done
}

ensure_dirs() {
  mkdir -p "$CLAIM_DIR" "$K6_RESERVE_DIR"
}

move_matches() {
  local pattern="$1"
  local target_dir="$2"
  local moved=0
  local src

  shopt -s nullglob
  for src in $pattern; do
    if [[ ! -f "$src" ]]; then
      continue
    fi

    local filename
    filename="$(basename "$src")"
    local target="$target_dir/$filename"

    if [[ "$src" == "$target" ]]; then
      continue
    fi

    if [[ "$DRY_RUN" == "true" ]]; then
      echo "[dry-run] mv '$src' '$target'" >&2
    else
      mv "$src" "$target"
      echo "[move] $src -> $target" >&2
    fi
    moved=$((moved + 1))
  done
  shopt -u nullglob

  echo "$moved"
}

main() {
  parse_args "$@"
  ensure_dirs

  echo "[organize-raw] raw_root=$RAW_ROOT"
  echo "[organize-raw] claim_dir=$CLAIM_DIR"
  echo "[organize-raw] k6_reserve_dir=$K6_RESERVE_DIR"

  local claim_moved
  local k6_moved

  claim_moved="$(move_matches "$RAW_ROOT/claim-v2-*" "$CLAIM_DIR" | tail -n1)"
  k6_moved="$(move_matches "$RAW_ROOT/reserve-remaining-k6-*" "$K6_RESERVE_DIR" | tail -n1)"

  echo "[organize-raw] moved_claim_files=$claim_moved"
  echo "[organize-raw] moved_k6_files=$k6_moved"
}

main "$@"
