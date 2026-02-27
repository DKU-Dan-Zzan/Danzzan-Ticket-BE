# Claim v2 로컬 부하테스트 실행 가이드

## 1) 목적/판정 기준
- 대상: Redis Lua v2 기반 Claim 경로 (`POST /tickets/request`)
- 고정 시나리오:
  - init stock = `1000`
  - 동시 요청 사용자 수 = `1500` (서로 다른 `userId`)
- 기대 집계:
  - `SUCCESS=1000`, `SOLD_OUT=500`, `ALREADY=0`
- 성능/안정성 기준:
  - `p95 <= 2000ms`
  - 오류율 `0%`

PASS 조건:
- 상태 집계가 기대값과 정확히 일치
- `p95 <= 2000ms`
- request 오류율 `0%`

## 2) 필수 환경
- Spring profile: `local-compose`
- DB/캐시:
  - MySQL 접근 가능 (`application-local-compose.yml` 기준 `localhost:13306`)
  - Redis 접근 가능 (`localhost:6379`)
- 애플리케이션 실행:
```bash
SPRING_PROFILES_ACTIVE=local-compose ./gradlew bootRun
```
- 관리자 계정(개발용 시드):
  - `studentId=1234`, `password=1234`

## 3) 실행 순서
1. 관리자 토큰 발급
```bash
ADMIN_TOKEN="$(curl -s -X POST http://localhost:8080/user/login \
  -H 'Content-Type: application/json' \
  -d '{"studentId":"1234","password":"1234"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
```

2. 재고 초기화 + 부하 실행
```bash
ADMIN_TOKEN="$ADMIN_TOKEN" scripts/loadtest_ticket_claim.sh \
  --init \
  --event-id festival-day1 \
  --stock 1000 \
  --users 1500 \
  --concurrency 200 \
  --base-url http://localhost:8080
```

3. 최근 CSV 확인
```bash
LATEST_CSV="$(ls -t reports/loadtest/raw/claim-v2-*.csv | head -n1)"
echo "$LATEST_CSV"
```

4. 결과 집계/판정
```bash
scripts/aggregate_ticket_claim_results.sh \
  --input "$LATEST_CSV" \
  --expected-success 1000 \
  --expected-sold-out 500 \
  --expected-already 0 \
  --max-p95-ms 2000
```

## 4) 산출물 경로
- Raw 결과:
  - `reports/loadtest/raw/claim-v2-*.csv`
  - `reports/loadtest/raw/claim-v2-*.json`
- 판정 리포트:
  - `reports/loadtest/claim-v2-*.md`

## 5) dry-run / 샘플 검증
- 부하 스크립트 dry-run:
```bash
scripts/loadtest_ticket_claim.sh --dry-run --users 10 --concurrency 2
```

- 집계 샘플 검증:
```bash
scripts/aggregate_ticket_claim_results.sh \
  --input scripts/samples/claim-loadtest-sample.csv \
  --expected-success 3 \
  --expected-sold-out 2 \
  --expected-already 0
```

## 6) 주의사항
- `reports/loadtest/raw/*` 원시 산출물은 `.gitignore` 대상이다.
- `reports/loadtest/*.md` 리포트는 필요 시 커밋 가능하다.
- 기준 미달(`FAIL`) 시 원인 메모를 리포트에 남기고, 기능 코드 수정은 별도 브랜치에서 진행한다.
