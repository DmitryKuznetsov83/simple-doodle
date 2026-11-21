# Mini Doodle

## Idea

A user's calendar is an append-only **event log**: `MARKED_FREE`, `MARKED_BUSY`, `MEETING_BOOKED`,
each on a time range. What the user sees is a **projection** of that log: events are applied in order,
later ones override earlier ones, time not covered by any event is `BUSY`.

The write and read sides are separate services behind one gateway:

- **Booking App** (write side) - creates users, validates every command against the booking rules
  and appends events
- **Calendar App** (read side, read-only) - builds the projection of a user's calendar for a time window
- **Gateway** - single entry point, routes `/booking/**` and `/calendar/**`

Details (diagrams, DB schema, booking rules, assumptions): [docs/spec.md](docs/spec.md).

## How to run

```shell
docker compose up -d --build
```

- Gateway (entry point): http://localhost:8080
- Booking App: http://localhost:8081, Calendar App: http://localhost:8082 (exposed for debugging)
- PostgreSQL: `localhost:5433`, database / user / password `doodle`

The schema and test users (`John`, `Paul`, `George`, `Ringo` with ids 1-4) are created by the init scripts
in `db/` on the first start of an empty volume. To recreate the database: `docker compose down -v`.

## How to use

The API is command-based: every change is a `POST` that appends an event, there are no `PUT` / `DELETE`.
A slot is not an entity with an id, it is just an interval in the `FREE` state:

| task                      | request                                            | parameters (JSON body unless noted)                                                     |
|---------------------------|----------------------------------------------------|-----------------------------------------------------------------------------------------|
| create a user             | `POST /booking/users`                              | `name`                                                                                  |
| create a slot (mark free) | `POST /booking/users/{userId}/free`                | `startTime`, `endTime`                                                                  |
| delete a slot (mark busy) | `POST /booking/users/{userId}/busy`                | `startTime`, `endTime`                                                                  |
| modify a slot             | `busy` on the old interval + `free` on the new one | as above                                                                                |
| book a meeting            | `POST /booking/users/{userId}/meetings`            | `title`, `description` (optional), `participantIds` (may be empty), `startTime`, `endTime` |
| view free / busy calendar | `GET /calendar/users/{userId}`                     | query: `startTime`, `endTime`                                                           |
| view a meeting            | `GET /calendar/meetings/{meetingId}`               | -                                                                                       |

`userId` in a booking request is the user who acts: the slot owner or the meeting organizer.
Times are ISO-8601 local date-times, e.g. `2027-03-01T09:00`.

Rules:
- Times are local (no time zones), aligned to **15 minutes**, ranges are `[start, end)`, start in the future
- A meeting can be booked only if the whole range is `FREE` for every participant (the organizer is added
  automatically); a free / busy mark is rejected if it overlaps a meeting. Meetings cannot be cancelled
- The calendar window is up to 31 days
- Errors are returned as [Problem Details](https://www.rfc-editor.org/rfc/rfc9457):
  `400` invalid input, `404` unknown user / meeting, `422` unknown meeting participants,
  `409` booking rule violated or the user name is taken

## Design decisions

- **Event log instead of slot CRUD** - one append-only table covers create / delete / modify slots
  and meetings; overlapping and splitting slots need no special handling, the history is kept
- **Separate write and read sides** - reads (calendars) are expected to dominate, Calendar App is read-only
  and can be scaled independently (e.g. pointed to a read replica)
- **Scale** - events are indexed by `(user_id, start_time)`, a calendar is projected only for the requested
  window, the window is limited
- **Concurrency** - a command locks the rows of the involved users (`SELECT ... ORDER BY id FOR UPDATE`),
  then reads events and checks the rules: no double booking, no deadlocks
- **Core module** - the projection and booking rules are pure Java without Spring, shared by both apps

## Things to improve

- **Cached projections** - keep projections in a cache instead of replaying the log on every read.

  ```mermaid
  flowchart LR
      client([Client]) --> gw[API Gateway]

      gw -- "/calendar/**" --> cal[Calendar App]
      gw -- "/booking/**" --> bkg[Booking App]

      subgraph db [PostgreSQL]
          event[(event)]
          outbox[(outbox)]
      end

      bkg -- "one transaction:<br/>append events + outbox rows" --> db
      outbox -- "1. poll new rows" --> proj[Projector]
      proj -- "2. apply events,<br/>update snapshots" --> cache[(Cache)]
      proj -. "3. delete processed rows" .-> outbox
      cache -- reads --> cal
  ```

  Assumption: **reads use the cache only**, so the calendar may briefly lag behind writes;
  booking rules are still checked against the database

- **Meeting update / delete** - is a must for real product
- **GraphQL instead of REST** - the API is command-based: nothing is ever updated or addressed by id,
  only events are appended.
