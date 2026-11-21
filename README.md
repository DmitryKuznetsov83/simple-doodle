## Idea

## How to run the project

```shell
docker compose up -d --build
```

Services:
- Booking App: http://localhost:8081
- Calendar App: http://localhost:8082
- PostgreSQL: `localhost:5432`, database / user / password `doodle`

Database schema is created by the init scripts in `db/` on the first start of an empty volume.
To recreate the database: `docker compose down -v`.
