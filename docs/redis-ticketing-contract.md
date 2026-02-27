# Redis Ticketing Contract (Queue-Ready, Claim v2)

## Scope
- This document defines Redis key naming and request API behavior with Claim v2.
- Queue admission extension is out of scope in this sprint.

## Key Naming
- Prefix: `ticket:{eventId}:...`
- Segment type:
  - `eventId`, `userId` are handled as string segments.
  - Numeric IDs must be converted to string before key build.
- Escaping rule:
  - If `eventId` or `userId` contains `:`, replace `:` with `%3A` in key segments.
  - Implemented in `TicketRedisKeys`.

### Keys used in this sprint
- `stock`: `ticket:{eventId}:stock` (INT)
- `user`: `ticket:{eventId}:user:{userId}` (STRING/INT)
- `status`: `ticket:{eventId}:status:{userId}` (STRING: `WAITING|ADMITTED|SUCCESS|SOLD_OUT|ALREADY`)

### Reserved keys for queue extension
- `queue`: `ticket:{eventId}:queue` (ZSET)
- `gate`: `ticket:{eventId}:gate` (SET or ZSET)

## API Contract

> Existing route conventions are preserved:
> - Admin prefix: `/api/admin`
> - Ticket prefix: `/tickets`

### 1) 관리자 초기화
- `POST /api/admin/ticket/init`
- Request
```json
{
  "eventId": "festival-day1",
  "stock": 5000
}
```
- Operational rule:
  - `stock` is rewritten for the event.
  - Existing claim artifacts are cleaned before open:
    - `ticket:{eventId}:user:*`
    - `ticket:{eventId}:status:*`
  - Cleanup uses `SCAN + UNLINK` (no `KEYS`).
- Response (contract)
```json
{
  "eventId": "festival-day1",
  "stock": 5000
}
```

### 2) 티켓 요청(선착순)
- `POST /tickets/request`
- Request
```json
{
  "eventId": "festival-day1",
  "userId": "32221902"
}
```
- Response (contract)
```json
{
  "status": "SUCCESS",
  "remaining": 42
}
```
- Response status cases
  - `SUCCESS` with `remaining` value
  - `SOLD_OUT` with `remaining: null`
  - `ALREADY` with `remaining: null`

### 3) 상태 조회 (polling)
- `GET /tickets/status?eventId=festival-day1&userId=32221902`
- Response (contract)
```json
{
  "status": "NONE"
}
```
- If status key does not exist, return `NONE`.

### Status enum
- `NONE`, `WAITING`, `ADMITTED`, `SUCCESS`, `SOLD_OUT`, `ALREADY`

## Service Boundary
- `TicketRequestStatus` is the only status type across request flow.
- `AdmissionService` responsibility:
  - Emits admission status only (`WAITING`, `ADMITTED`).
  - Current stub implementation always returns `ADMITTED`.
- `ClaimService` responsibility:
  - Emits claim result only (`SUCCESS`, `SOLD_OUT`, `ALREADY`).
  - `ClaimResult` signature is fixed to:
    - `status: TicketRequestStatus`
    - `remaining: Long?`
  - `remaining` is used only when `status=SUCCESS`.
  - `remaining` must be `null` when `status=SOLD_OUT|ALREADY`.
- `TicketController` request flow is fixed:
  - `admit` first.
  - If admission status is not `ADMITTED`, do not call claim and return that status as-is.
  - Only when `ADMITTED`, call claim and return `ClaimResult`.
- Claim v2 behavior (Lua atomic):
  - A single Lua script performs claim decision, stock mutation, user marker write, and status write atomically.
  - Decision order is fixed:
    - If `userKey` already exists -> `ALREADY`
    - Else if `stock <= 0` (or stock key missing/invalid) -> `SOLD_OUT`
    - Else `DECR(stockKey)` + `SET(userKey)` + `SET(statusKey=SUCCESS)` -> `SUCCESS`
  - Script return payload is fixed to `[code, remaining]`.
    - `code=1` -> `ALREADY`
    - `code=2` -> `SOLD_OUT`
    - `code=3` -> `SUCCESS`
  - API response rule remains unchanged:
    - `SUCCESS` uses `remaining`.
    - `SOLD_OUT|ALREADY` must return `remaining: null`.
- Claim observability:
  - `ClaimService` logs each outcome as `claim_v2 outcome` with `eventId`, `userId`, `status`, `remaining`, `total`.
  - `total` is an in-memory per-status counter (`SUCCESS|SOLD_OUT|ALREADY`) for runtime visibility.
- Queue extension rule:
  - When queue admission is introduced, replace `AdmissionService` only.
  - Reuse `ClaimService` interface without signature changes.
- Error extension point:
  - `errorCode` is reserved for future extension and intentionally not included in current response schema.

## Implementation Note
- `POST /api/admin/ticket/init`: implemented (stock rewrite + claim key cleanup via scan/unlink).
- `POST /tickets/request`: implemented (`admit -> claim`) with Lua v2 atomic claim.
- `GET /tickets/status`: implemented (status read, missing key => `NONE`).
- Claim status persistence (`SUCCESS|SOLD_OUT|ALREADY`) is unified inside Lua.
