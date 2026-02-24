# Redis Ticketing Contract (Queue-Ready)

## Scope
- This document defines only Redis key naming and API contract scaffolding.
- Real issue logic (`DECR`, `SETNX`, Lua script, queue admission) is out of scope in this sprint.

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
- `status`: `ticket:{eventId}:status:{userId}` (STRING: `SUCCESS|SOLD_OUT|ALREADY|WAITING|ADMITTED`)

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

### 3) 상태 조회 (polling)
- `GET /tickets/status?eventId=festival-day1&userId=32221902`
- Response (contract)
```json
{
  "status": "WAITING"
}
```

### Status enum
- `NONE`, `WAITING`, `ADMITTED`, `SUCCESS`, `SOLD_OUT`, `ALREADY`

## Implementation Note
- Current endpoints are scaffold-only and return `501 Not Implemented`.
