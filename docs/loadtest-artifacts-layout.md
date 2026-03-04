# Loadtest 산출물 디렉토리 규칙

## 목적
- 부하테스트 산출물의 위치와 파일명을 시나리오별로 고정해 재실행/비교/보관을 단순화한다.

## 표준 디렉토리
- Raw 산출물(로그/CSV/JSON):
  - `reports/loadtest/raw/claim-v2/`
  - `reports/loadtest/raw/k6/reserve-remaining/`
- 분석 리포트(Markdown):
  - `reports/loadtest/reports/claim-v2/`
  - `reports/loadtest/reports/k6/reserve-remaining/` (향후 확장)
- 아카이브:
  - `reports/loadtest/archive/claim-v2/`
  - `reports/loadtest/archive/k6/reserve-remaining/` (향후 확장)

## 파일명 규칙
- Claim v2 raw:
  - `claim-v2-YYYYMMDD-HHMMSS.csv`
  - `claim-v2-YYYYMMDD-HHMMSS.json`
- Claim v2 report:
  - `claim-v2-YYYYMMDD-HHMMSS.md`
- Reserve/Remaining(k6) raw:
  - `reserve-remaining-k6-YYYYMMDD-HHMMSS.log`
  - `reserve-remaining-k6-YYYYMMDD-HHMMSS-summary.json`
  - `reserve-remaining-k6-YYYYMMDD-HHMMSS-k6-summary.json`

## 운영 규칙
- 최신 PASS 리포트 1건을 기준 리포트로 유지한다.
- FAIL 리포트는 archive로 이동한다.
- 필요 시 오래된 PASS도 archive로 이동한다.

## 정리 스크립트
```bash
# raw 산출물(flat -> scenario dir) 정리
scripts/organize_loadtest_raw_artifacts.sh

# claim-v2 리포트 정리 + legacy flat 리포트 자동 이관
scripts/organize_loadtest_reports.sh

# FAIL + 오래된 PASS까지 정리
scripts/organize_loadtest_reports.sh --archive-old-pass
```
