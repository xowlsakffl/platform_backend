# 백엔드 디렉토리 구조

작성 기준: 2026-07-28

이 문서는 `medi_backend`의 실제 디렉토리 배치 기준을 정의한다.

## 1. 루트 구조

```text
medi_backend/
  AGENTS.md
  README.md
  build.gradle
  settings.gradle
  gradle/
  src/
    main/
      java/
      resources/
    test/
  docs/
```

- `AGENTS.md`: 에이전트 작업 기준
- `README.md`: 실행과 전체 진입 문서
- `build.gradle`: Spring Boot/Gradle 의존성
- `src/main/java`: 애플리케이션 코드
- `src/main/resources`: 설정, Flyway migration
- `docs`: 설계 문서

현재 단계에서는 테스트 코드를 추가하지 않는다.

## 2. Java 패키지 구조

```text
src/main/java/com/medi/
  MediBackendApplication.java
  adapter/
    in/
      web/
        publicapi/
        staff/
        hospital/
        beauty/
        user/
  application/
    account/
    hospital/
      command/
      query/
      result/
    category/
    media/
    operationhistory/
    content/
    notification/
  domain/
    account/
    common/
    hospital/
    category/
    media/
    operationhistory/
    content/
    notification/
  infrastructure/
    persistence/
      account/
      hospital/
      category/
      operationhistory/
    redis/
    storage/
    messaging/
    scheduler/
  common/
    config/
    error/
    security/
    web/
```

## 3. 패키지 책임

| 패키지 | 책임 |
| --- | --- |
| `adapter.in.web` | Controller, HTTP Request DTO |
| `application` | Service, Command, Query, Result |
| `domain` | Entity, enum, 도메인 규칙 |
| `infrastructure` | Repository, Redis, storage, messaging, scheduler 구현 |
| `common` | 공통 설정, 응답, 예외, 보안, request trace |

## 4. Actor별 Controller 배치

Controller는 URL actor 기준으로 둔다.

```text
adapter/in/web/publicapi
adapter/in/web/staff
adapter/in/web/hospital
adapter/in/web/beauty
adapter/in/web/user
```

도메인이 커지면 actor 아래에 도메인 폴더를 만든다.

```text
adapter/in/web/staff/hospital/controller
adapter/in/web/staff/hospital/request
```

Controller에는 다음만 둔다.

- route mapping
- request binding
- Bean Validation
- Request DTO에서 Command/Query 변환 호출
- Service 호출
- `ApiResponse` 반환

Controller에 비즈니스 규칙, DB 조회, 운영 히스토리 조립을 넣지 않는다.

## 5. Request DTO 배치

Request DTO는 Controller와 같은 adapter 하위에 둔다.

```text
adapter/in/web/staff/hospital/request/
  HospitalListRequest.java
  HospitalCreateRequest.java
  HospitalUpdateRequest.java
  HospitalStatusUpdateRequest.java
  HospitalAllowStatusUpdateRequest.java
```

기준:

- query parameter가 여러 개인 목록/검색은 Request DTO를 사용한다.
- request body가 있는 생성/수정/상태 변경은 Request DTO를 사용한다.
- 단일 `PathVariable` 조회/삭제는 Request DTO를 만들지 않는다.
- snake_case query parameter는 Request DTO에서 `@BindParam`으로 처리한다.

## 6. Application 배치

```text
application/hospital/
  HospitalStaffService.java
  command/
  query/
  result/
```

- `Service`: 유스케이스, 트랜잭션, 권한, 도메인 조합
- `command`: 생성/수정/상태 변경 입력
- `query`: 목록/검색 입력
- `result`: API 응답으로 내려갈 application 결과 모델

Request DTO는 application에 들어오면 안 된다. adapter에서 Command/Query로 바꿔 넘긴다.

## 7. Domain 배치

```text
domain/hospital/
  Hospital.java
  HospitalStatus.java
  HospitalAllowStatus.java
  HospitalContact.java
  HospitalContactType.java
```

Domain에는 다음을 둔다.

- JPA Entity
- enum
- 상태 변경 메서드
- 핵심 불변식
- 도메인 내부 연관 기준

조회 화면을 위한 조합 응답, HTTP validation, request parameter 이름은 domain에 넣지 않는다.

## 8. Infrastructure 배치

```text
infrastructure/persistence/hospital/
  HospitalRepository.java
  HospitalBusinessRegistrationRepository.java
  HospitalFeatureRepository.java
```

초기에는 Spring Data JPA repository를 infrastructure에 둔다.

외부 기술 구현은 모두 infrastructure로 보낸다.

- DB repository
- Redis cache adapter
- 파일 storage adapter
- queue/messaging adapter
- scheduler job

## 9. Resource 구조

```text
src/main/resources/
  application.yml
  application-local.yml
  db/
    migration/
      V1__create_hospital_core.sql
```

- 공통 설정은 `application.yml`
- 로컬 설정은 `application-local.yml`
- DB 변경은 Flyway migration으로 관리한다.
- 이미 적용된 migration은 수정하지 않고 새 migration을 추가한다.

## 10. 새 도메인 추가 절차

1. `docs/schema.dbml`과 도메인 상태 문서를 먼저 확인한다.
2. `domain/{domain}`에 Entity와 enum을 둔다.
3. `infrastructure/persistence/{domain}`에 Repository를 둔다.
4. `application/{domain}`에 Service, Command, Query, Result를 둔다.
5. `adapter/in/web/{actor}/{domain}`에 Controller와 Request DTO를 둔다.
6. 공통 응답과 예외 규칙에 맞춰 반환한다.
7. `./gradlew compileJava`로 검증한다.
