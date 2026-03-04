# Redis Ticketing Contract (Queue-Ready)

## Scope
- This document defines Redis key naming and v1 API behavior.
- Queue admission and Lua-based atomic claim are out of scope in this sprint.

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
- Claim v1 behavior:
  - First-claim check via `SETNX(userKey)`.
  - Stock decrement via `DECR(stockKey)`.
  - Negative/invalid stock paths return `SOLD_OUT`.
  - v1 is not Lua-atomic across all steps.
- Queue extension rule:
  - When queue admission is introduced, replace `AdmissionService` only.
  - Reuse `ClaimService` interface without signature changes.
- Error extension point:
  - `errorCode` is reserved for future extension and intentionally not included in current response schema.

## Implementation Note
- `POST /api/admin/ticket/init`: implemented (writes `stockKey`).
- `POST /tickets/request`: implemented (`admit -> claim`).
- `GET /tickets/status`: implemented (status read, missing key => `NONE`).
- Lua atomic claim migration is scheduled for v2.
