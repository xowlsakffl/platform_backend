# medi_backend

성형/뷰티 플랫폼의 Spring Boot 백엔드 단일 애플리케이션이다.

## 기술 기준

- Java 21
- Spring Boot 4
- Gradle
- MySQL 8
- Redis 6 이상
- Flyway
- Spring Data JPA
- Spring Security

## 로컬 위치

```text
/home/medi/medi_backend
```

## 문서

- [에이전트 작업 기준](./AGENTS.md)
- [백엔드 문서 인덱스](./docs/README.md)
- [백엔드 아키텍처](./docs/architecture.md)
- [디렉토리 구조](./docs/directory-structure.md)
- [구현 원칙](./docs/implementation-principles.md)
- [공통 API 응답](./docs/api-response.md)
- [도메인 상태 정의](./docs/domain-status-definition.md)
- [공통 Media 설계](./docs/media.md)
- [DBML 스키마](./docs/schema.dbml)

## 디렉토리 요약

```text
src/main/java/com/medi/
  adapter/        HTTP 진입점
  application/    유스케이스, Command, Query, Result
  domain/         엔티티, enum, 도메인 규칙
  infrastructure/ DB/Redis/storage/messaging 구현
  common/         공통 설정, 응답, 예외, 보안
src/main/resources/
  db/migration/   Flyway migration
docs/             백엔드 설계 문서
```

상세 기준은 [디렉토리 구조](./docs/directory-structure.md)를 따른다.

## 로컬 의존성

로컬 개발은 Docker 없이 WSL의 MySQL, Redis 서비스를 사용한다.

```bash
service mysql start
service redis-server start
```

기본 접속 정보:

```text
MySQL  localhost:3306 / db=medi / user=medi / password=medi
Redis  localhost:6379 / password=myStrongRedisPassword
```

환경 변수 예시는 [.env.example](./.env.example)을 참고한다.

로컬 웹 앱의 기본 CORS 허용 origin은 `localhost:3000`, `3001`, `3002`다. 배포 환경에서는 `CORS_ALLOWED_ORIGINS`에 실제 프론트 origin만 쉼표로 구분해 지정한다.

## 실행

```bash
./gradlew compileJava
./gradlew bootRun
```

## 최초 관리자 계정 생성

관리자 계정은 고정 비밀번호를 migration에 저장하지 않는다. 최초 한 번만 환경변수를 전달해 애플리케이션을 실행한다.

```bash
STAFF_BOOTSTRAP_ENABLED=true \
STAFF_BOOTSTRAP_EMAIL=admin@medi.local \
STAFF_BOOTSTRAP_PASSWORD='<강한 비밀번호>' \
./gradlew bootRun
```

기본 이름은 `Medi Admin`, 닉네임은 `admin`, 역할은 `platform.super_admin`이다. 필요하면 `STAFF_BOOTSTRAP_NAME`, `STAFF_BOOTSTRAP_NICKNAME`, `STAFF_BOOTSTRAP_ROLE`로 변경한다.

같은 이메일의 활성 계정이 이미 있으면 새 계정을 만들거나 비밀번호를 덮어쓰지 않고 역할 연결만 확인한다. 생성이 끝나면 다음 실행부터 `STAFF_BOOTSTRAP_ENABLED`를 제거하거나 `false`로 둔다.

현재 단계에서는 테스트 코드를 추가하지 않는다. 기능 검증은 컴파일, 애플리케이션 기동, 필요한 경우 수동 API 호출로 한다.

## API namespace

```text
/api/v1/public
/api/v1/staff
/api/v1/hospital
/api/v1/beauty
/api/v1/user
```

임시 서비스명은 화면 표시와 설정값에서만 사용한다. DB 테이블명과 API 리소스 경로에는 브랜드명을 넣지 않는다.

## 현재 구현 범위

- 공통 API 응답 포맷
- 공통 예외 처리
- request trace filter
- 기본 보안 설정
- JWT access token 기반 4개 actor 로그인/me/logout
- Staff role/permission 기반 권한 테이블
- MySQL/Flyway 연결
- Redis 연결 설정
- Hospital Staff API 1차 골격
- Hospital 관련 core schema
- Category Staff 목록/상세/생성/수정/삭제 API
- 사용처와 계층 탐색을 지원하는 Category selector API
- `HOSPITAL_MEDICAL` 3단계 기준 트리와 8개 사용처 초기 데이터
- 폴리모픽 Media 로컬 저장소와 Staff 업로드/조회/수정/삭제 API

## 개발 기준

- Controller는 얇게 유지한다.
- 복잡한 query parameter는 Request DTO로 받는다.
- 단일 `PathVariable` 조회/삭제는 별도 Request DTO를 만들지 않는다.
- Request DTO는 application 계층의 Command/Query로 변환한다.
- Service는 트랜잭션과 유스케이스 흐름을 담당한다.
- Repository는 infrastructure 계층에 둔다.
- 문서와 구현이 어긋나면 같은 작업에서 문서를 갱신한다.
