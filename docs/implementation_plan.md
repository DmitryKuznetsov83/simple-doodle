# Implementation plan

## 1. Project skeleton
- Multi-module Maven project (`core`, `booking-app`, `calendar-app`)
- `docker-compose.yml` with apps and PostgreSQL

## 2. Database schema
- Schema: tables, indexes, constraints

## 3. Users
- User creation (controller -> service -> repo) in booking-app

## 4. Core records
- `TimeRange` + validation
- `EventType`
- `TimeState`
- `Projection` 

## 5. Booking App: booking `FREE` and `BUSY` with trivial input validation
- `POST /booking/users/{userId}/free`
- `POST /booking/users/{userId}/busy`

## 6. Booking App: booking `MEETING` with trivial input validation
- `POST /booking/users/{userId}/meetings`

## 7. Build projection mechanism
- Pure function in `core`: events + window -> projection
- Apply events in order, later events override earlier ones
- Uncovered time is `BUSY`
- Merge adjacent intervals with the same state
- Clip to the requested window

## 8. Calendar App
- `GET /calendar/users/{userId}?startTime&endTime` - uses shared `Build projection` mechanism
- `GET /calendar/meetings/{meetingId}`

## 9. Booking App: state transition rules
- Apply business restriction before booking event
- One transaction per command: load events -> projection -> check rules -> append events

## 10. Booking App: concurrency 
- Lock `users` rows `ORDER BY id FOR UPDATE` at the start of the command transaction
- Concurrency test: parallel bookings of the same slot -> exactly one succeeds

## 11. Gateway
- API Gateway routing

## 12. End-to-end tests

## 13. Metrics

## 14. Documentation