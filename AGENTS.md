# Medi Backend 작업 기준

이 파일은 `medi_backend` 작업 시 가장 먼저 확인하는 에이전트 진입 문서다.

## 먼저 읽을 문서

1. [README](./README.md)
2. [문서 인덱스](./docs/README.md)
3. [백엔드 아키텍처](./docs/architecture.md)
4. [디렉토리 구조](./docs/directory-structure.md)
5. [인증 설계](./docs/authentication.md)
6. 변경 대상 도메인의 세부 문서

## 기본 원칙

- 문서는 한국어로 작성한다.
- 문서에는 외부 프로젝트명이나 이전 구현체명을 남기지 않는다.
- 백엔드는 Java 21, Spring Boot, Gradle 단일 애플리케이션으로 시작한다.
- 로컬 개발은 MySQL, Redis를 직접 사용한다. Docker는 기본 개발 경로로 두지 않는다.
- 테스트 코드는 현재 단계에서 추가하지 않는다.
- 검증은 우선 `./gradlew compileJava`와 필요한 경우 `./gradlew bootRun`으로 한다.
- `package-info.java`는 사용하지 않는다.

## 계층 기준

`Controller-Service-Repository` 흐름은 유지하되, 패키지는 아래 계층으로 나눈다.

- `adapter.in.web`: HTTP 진입점. Controller와 HTTP Request DTO를 둔다.
- `application`: 유스케이스. Service, Command, Query, Result를 둔다.
- `domain`: 엔티티, 값 객체, enum, 핵심 상태 전이 규칙을 둔다.
- `infrastructure`: DB, Redis, storage, messaging 같은 외부 연동 구현을 둔다.
- `common`: 전역 설정, 공통 응답, 예외, 보안, 웹 공통 기능을 둔다.

## HTTP 입력 모델 기준

- `PathVariable` 하나로 끝나는 조회/삭제는 Request DTO를 만들지 않는다.
- query parameter가 여러 개이고 검색 조건/페이징/정렬을 이룰 때는 `*ListRequest`, `*SearchRequest`를 사용한다.
- request body가 있는 생성/수정/상태 변경은 `*CreateRequest`, `*UpdateRequest`, `*StatusUpdateRequest`를 사용한다.
- Controller는 Request DTO를 application 계층의 `Command` 또는 `Query`로 변환해서 Service에 넘긴다.
- Service는 adapter 계층의 Request DTO를 직접 의존하지 않는다.
- 응답은 application 계층의 `Result`를 공통 `ApiResponse`로 감싼다.

## API namespace

- Public API: `/api/v1/public`
- Staff API: `/api/v1/staff`
- Hospital API: `/api/v1/hospital`
- Beauty API: `/api/v1/beauty`
- User API: `/api/v1/user`

## 문서 갱신 규칙

- 패키지 구조가 바뀌면 [디렉토리 구조](./docs/directory-structure.md)를 갱신한다.
- 요청/응답 규칙이 바뀌면 [API 응답 문서](./docs/api-response.md)와 관련 도메인 문서를 갱신한다.
- DB 컬럼, enum, 상태값이 바뀌면 [DBML](./docs/schema.dbml)과 [도메인 상태 정의서](./docs/domain-status-definition.md)를 같이 갱신한다.
- 구현과 문서가 충돌하면 먼저 실제 구현 기준으로 문서를 정리한 뒤 코드를 수정한다.
