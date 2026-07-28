# medi_backend

Spring Boot API for the Medi beauty platform.

## Requirements

- Java 21
- Docker, optional for local PostgreSQL and Redis

## Local Run

```bash
./gradlew test
./gradlew bootRun
```

The local profile expects PostgreSQL and Redis at:

```text
PostgreSQL  localhost:5432 / db=medi / user=medi / password=medi
Redis       localhost:6379
```

Start local dependencies:

```bash
docker compose up -d postgres redis
```

## Public Endpoint

```text
GET /api/v1/public/app-config
```

## API Namespaces

```text
/api/v1/public
/api/v1/user
/api/v1/staff
/api/v1/hospital
/api/v1/beauty
```

The visible brand name is configuration. Do not put `medi` into database table names or API domain paths.
