# medi_backend

Medi 성형/뷰티 플랫폼의 Spring Boot 백엔드입니다.

## 요구 사항

- Java 21
- Docker: 로컬 PostgreSQL, Redis 실행 시 필요

## 문서

- [에이전트 작업 규칙](./AGENTS.md)
- [백엔드 문서 인덱스](./docs/README.md)
- [백엔드 아키텍처](./docs/architecture.md)
- [Beaulab에서 Medi로 이관할 때의 기준](./docs/migration-from-beaulab.md)

## 로컬 실행

```bash
./gradlew test
./gradlew bootRun
```

로컬 프로필은 아래 접속 정보를 기본값으로 사용합니다.

```text
PostgreSQL  localhost:5432 / db=medi / user=medi / password=medi
Redis       localhost:6379
```

로컬 의존성 실행:

```bash
docker compose up -d postgres redis
```

## 공개 엔드포인트

```text
GET /api/v1/public/app-config
```

## API 네임스페이스

```text
/api/v1/public
/api/v1/user
/api/v1/staff
/api/v1/hospital
/api/v1/beauty
```

화면에 노출되는 브랜드명은 설정값으로 관리합니다. DB 테이블명이나 API 도메인 경로에는 `medi`를 넣지 않습니다.
