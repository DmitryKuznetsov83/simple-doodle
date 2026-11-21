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
- Events are applied as is, booking rules are not checked here (step 10)
- Uncovered time is `BUSY`
- Merge adjacent intervals with the same state; different meetings next to each other are not merged
- Clip to the requested window
- Painting algorithm is taken from a library (Guava `RangeMap`) behind our own interface,
  so it can be replaced later with a custom or another implementation

## 8. Calendar App: user's calendar
- Read-only, SQL via `JdbcClient`
- `GET /calendar/users/{userId}?startTime&endTime` - uses shared `Projection builder` mechanism
  - `startTime`, `endTime` required; window up to 31 days (configurable)
  - `MEETING` intervals include `meetingId` and `meetingTitle`
- Tests: `@WebMvcTest` + Testcontainers for SQL

## 9. Calendar App: meeting
- Read-only, SQL via `JdbcClient`, one query
- `GET /calendar/meetings/{meetingId}` - organizer, time and participants are taken from the meeting's events
  - Organizer is `creator_id`, participants (`[{id, name}]`) are `user_id`, organizer first
  - No events for the meeting -> 404
- Tests: `@WebMvcTest` + Testcontainers for SQL

## 10. Booking App: state transition rules
- Apply business restriction before booking event
- One transaction per command: load events -> projection -> check rules -> append events
- Projection window = requested interval; rules (pure check in `core`):
  - `MARKED_FREE` / `MARKED_BUSY`: no `MEETING` in the projection, partial overlap rejects the whole request
  - `MEETING_BOOKED`: the projection is a single `FREE` interval for every participant, organizer included
- Events of all participants in one query (`user_id IN (:ids)`, overlapping the interval, `ORDER BY id`)
- Rules are checked before the `meeting` row is saved
- Violation -> 409 Conflict; for a meeting - ids of participants who are not free
- Tests: core unit tests of the rules, `@WebMvcTest` (201 / 409), Testcontainers for the new query

## 11. Booking App: concurrency 
- Lock `users` rows `ORDER BY id FOR UPDATE` at the start of the command transaction
- Concurrency test: parallel bookings of the same slot -> exactly one succeeds

## 12. Gateway
- API Gateway routing

## 13. End-to-end tests

## 14. Metrics

## 15. Documentation