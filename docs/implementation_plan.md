# Implementation plan

## 1. Project skeleton
- Multi-module Maven project (`core`, `booking-app`, `calendar-app`)
- `docker-compose.yml` with apps and PostgreSQL

## 2. Database schema
- Schema: tables, indexes, constraints

## 3. Create user
- User creation (controller -> service -> repo) in booking-app

## 4. Core records
- Pure Java, no Spring; invariants are validated in constructors
- These records are the internal domain model, not DTOs
- `TimeRange` - (startTime, endTime)
- `TimeState` - `FREE`, `BUSY`, `MEETING`
- `EventType` - `MARKED_FREE`, `MARKED_BUSY`, `MEETING_BOOKED`
- `Event` - (eventType, timeRange, meetingId)
- `Projection` - (window, intervals)
- `ProjectionInterval` - (timeRange, state, meetingId)

## 5. Booking App: booking `FREE` and `BUSY` with trivial input validation
- `POST /booking/users/{userId}/free`
- `POST /booking/users/{userId}/busy`

## 6. Booking App: booking `MEETING` with trivial input validation
- `POST /booking/users/{userId}/meetings`

## 7. Projection builder
- Pure function in `core`: events + window -> projection
- Apply events in order, later events override earlier ones
- Preparing events is the caller's responsibility: events of a single user, in log order (by `id`)
- Events are applied as is, booking rules are not checked here (step 9)
- Uncovered time is `BUSY`
- Merge adjacent intervals with the same state; different meetings next to each other are not merged
- Clip to the requested window
- Painting algorithm is taken from a library (Guava `RangeMap`) behind our own interface,
  so it can be replaced later with a custom or another implementation

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