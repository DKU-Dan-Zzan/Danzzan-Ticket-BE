# Reserve + Remaining 대규모 부하테스트 (k6)

## 1) 목표 시나리오
- 가상 유저: `2000`
- 각 유저: 로그인 토큰 보유 후 `POST /tickets/{eventId}/reserve` 1회 시도 + `GET /tickets/events` 폴링 반복
- 총 시간: `40s`
  - `0~5s`: 0 -> 2000 ramp-up
  - `5~35s`: 2000 유지
  - `35~40s`: 2000 -> 0 ramp-down
- 목표 총 요청 수: `171,250` (실측은 응답속도/폴링간격에 따라 변동)

> 참고: 현재 백엔드에는 `/remaining` 엔드포인트가 없어서, 잔여 수량 폴링은 `GET /tickets/events`를 사용합니다.

## 2) 사전 준비
### A. k6 설치
macOS 예시:
```bash
brew install k6
```

### B. 앱 실행
```bash
SPRING_PROFILES_ACTIVE=local-compose ./gradlew bootRun
```

### C. 2000 부하테스트 계정 자동 시드(옵션)
`DevDataInitializer`가 아래 env가 켜져 있으면 시작 시 계정을 생성합니다.

```bash
export LOADTEST_USERS_ENABLED=true
export LOADTEST_USER_COUNT=2000
export LOADTEST_USER_PREFIX=loadtest-
export LOADTEST_USER_PASSWORD=loadtest1234!
SPRING_PROFILES_ACTIVE=local-compose ./gradlew bootRun
```

생성되는 로그인 계정 예:
- `loadtest-000001`
- `loadtest-000002`
- ...
- `loadtest-002000`

## 3) 실행
```bash
scripts/run_reserve_remaining_k6.sh \
  --base-url http://localhost:8080 \
  --event-id 1 \
  --user-count 2000 \
  --max-vus 2000 \
  --ramp-up 5s \
  --hold 30s \
  --ramp-down 5s \
  --poll-interval-sec 0.45 \
  --expected-total-requests 171250
```

## 4) 산출물
기본 경로: `reports/loadtest/raw/`

실행 시 아래 파일이 생성됩니다.
- `reserve-remaining-k6-<timestamp>.log`
- `reserve-remaining-k6-<timestamp>-summary.json` (스크립트 커스텀 요약)
- `reserve-remaining-k6-<timestamp>-k6-summary.json` (k6 summary export)

## 5) 자주 조정하는 파라미터
- 폴링 강도 조절: `--poll-interval-sec`
  - 총 요청 수가 목표보다 적으면 값을 낮춤
  - 총 요청 수가 목표보다 많으면 값을 높임
- 이벤트 경로 변경:
  - reserve 경로를 바꾸려면 `--reserve-path`
  - remaining 경로를 바꾸려면 `--remaining-path`

## 6) 빠른 점검
실행 전에 아래 두 로그인 계정을 precheck 합니다.
- 첫 계정: `<prefix>000001`
- 마지막 계정: `<prefix><user_count 6자리>`

precheck 실패 시 계정 시드(env)와 앱 재시작 여부를 확인하세요.
