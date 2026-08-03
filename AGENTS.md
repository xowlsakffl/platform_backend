# Platform Backend 작업 기준

이 파일은 백엔드 작업의 에이전트 진입 문서다.

## 먼저 읽기

1. [README](./README.md)
2. [아키텍처와 디렉터리](./docs/architecture.md)
3. [인증과 권한](./docs/authentication.md)
4. [도메인 정책과 상태](./docs/domain-status-definition.md)
5. 변경 대상의 세부 문서와 실제 코드

## 정책 우선순위

1. 사용자가 명시적으로 확정한 정책
2. 이 저장소의 최신 문서
3. 현재 코드의 실제 비즈니스 동작

정책과 코드가 충돌하면 임의로 기존 동작을 유지하지 않고 최신 확정 정책을 기준으로 영향 범위를 확인한다. Request 검증, 권한, 상태 전이, 트랜잭션, 조회 조건, 응답 조립, 삭제 부수 효과를 함께 점검한다.

## 도메인 구현 절차

1. 관련 문서, Controller, Request, Command/Query, DTO, Policy, Entity, migration을 확인한다.
2. 확정 정책과 현재 구현의 차이 및 영향 범위를 정리한다.
3. 현재 문서에 상태, 권한, 입력, 공개 범위, 삭제 정책을 반영한다.
4. HTTP Request와 Bean Validation을 구현한다.
5. application의 Command/Query, Service, Result를 구현한다.
6. domain 엔티티와 상태 규칙, infrastructure Repository를 구현한다.
7. 예외, 이력, 캐시 무효화, 미디어 수명주기를 점검한다.
8. 테스트를 제외한 빌드, 기동, 수동 API 호출로 검증한다.

## 구현 원칙

- Java 21, Spring Boot, Gradle 단일 애플리케이션을 유지한다.
- 로컬 개발은 MySQL과 Redis 서비스를 직접 사용하며 Docker를 기본 경로로 두지 않는다.
- 패키지는 도메인과 계층으로 나누고, Actor는 `adapter.in.web`의 API 진입점에서 분리한다.
- Controller는 입력 변환과 응답 포장만 담당한다.
- application Service는 유스케이스, 트랜잭션, 권한 흐름을 담당한다.
- domain은 엔티티, enum, 상태 전이 규칙을 담당한다.
- infrastructure는 JPA, Redis, 파일 저장소 같은 외부 연동을 담당한다.
- `package-info.java`는 만들지 않는다.
- 테스트 코드는 현재 단계에서 추가하지 않는다.
- 인증 변경은 `Staff`, `Partner`, `User` 세 Actor의 경로, 세션 만료, 쿠키 경로, CORS를 함께 점검한다.
- 웹 access token과 인증 사용자 정보는 브라우저 영구 저장소에 기록하지 않는다.
- refresh token 원문은 DB와 로그에 남기지 않고 HttpOnly 쿠키로만 전달한다.

## HTTP 모델

- 경로 ID만 필요한 단순 조회·삭제는 별도 Request를 만들지 않는다.
- 필터, 정렬, 페이징, include가 있는 조회는 `*Request`에서 `*Query`로 변환한다.
- 생성·수정·상태 변경은 `*Request`에서 `*Command`로 변환한다.
- application 계층은 adapter의 Request 클래스를 참조하지 않는다.
- 응답은 application의 `*Result`를 `ApiResponse`로 감싼다.

## 예외

- 타입, 길이, 형식, 범위는 Bean Validation으로 검증한다.
- 중복, 상태 전이, 소유권, 조회 실패는 `ApiException`과 `ErrorCode`를 사용한다.
- 저장 데이터 손상, 직렬화 실패, 파일 I/O 같은 내부 실패는 `InternalApplicationException`을 사용한다.
- application 정책 실패에 `IllegalStateException`이나 일반 `RuntimeException`을 직접 사용하지 않는다.
- Controller에서 예외를 잡아 JSON을 직접 만들지 않는다.

## 변경 안전성

- Partner·Specialist·Category·Media 변경은 하나의 트랜잭션 경계를 검토한다.
- 미디어 신규 파일은 롤백 시 삭제하고, 교체된 파일은 DB 커밋 뒤 삭제한다.
- soft delete 대상의 연결 미디어와 카테고리 할당, 운영 이력 부수 효과를 함께 점검한다.
- 목록의 파생 집계값을 임의 상수로 추가하지 않는다. 의존 도메인이 없으면 미구현 사실을 문서화한다.
- 문서에는 현재 구현처럼 오해할 수 있는 미래 기능 설명을 넣지 않는다.
