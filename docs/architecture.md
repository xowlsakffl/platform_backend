# 백엔드 아키텍처

작성 기준: 2026-07-28

이 문서는 백엔드 구현의 기본 구조를 정의한다. 현재 기준은 Spring Boot 단일 애플리케이션이며, `Controller-Service-Repository` 흐름을 유지하되 도메인별 패키지 경계를 분명히 둔다.

## 1. 아키텍처 방향

초기 구조는 모듈러 모놀리스다.

- 배포 단위는 하나의 Spring Boot 애플리케이션이다.
- 코드는 actor가 아니라 도메인 중심으로 나눈다.
- actor는 URL prefix, 인증 주체, 권한 정책을 구분하는 값이다.
- 비즈니스 흐름은 application service가 조립한다.
- 상태 전이와 핵심 불변식은 domain 객체가 갖는다.
- DB, Redis, storage, messaging 구현은 infrastructure로 밀어낸다.

## 2. 계층

```text
adapter.in.web
  -> Controller, HTTP Request DTO

application
  -> Service, Command, Query, Result

domain
  -> Entity, enum, value object, 도메인 규칙

infrastructure
  -> Repository, DB/Redis/storage/messaging 구현

common
  -> 설정, 공통 응답, 공통 예외, 보안, 웹 공통 기능
```

Spring 관점에서는 Controller, Service, Repository 구조가 맞다. 다만 Request DTO, Command/Query, Result, Entity가 한 패키지에 섞이지 않게 분리한다.

## 3. API namespace

```text
/api/v1/public
/api/v1/staff
/api/v1/hospital
/api/v1/beauty
/api/v1/user
```

- `public`: 로그인 전 또는 공개 데이터
- `staff`: 내부 운영자
- `hospital`: 병원 파트너
- `beauty`: 뷰티 파트너
- `user`: 일반 앱/웹 사용자

API 경로는 리소스 기준으로 설계한다. 화면 메뉴명과 API path가 반드시 같을 필요는 없다.

## 4. 요청 처리 흐름

1. Controller가 HTTP 요청을 바인딩한다.
2. Bean Validation으로 입력값을 검증한다.
3. Request DTO를 Command 또는 Query로 변환한다.
4. Application Service가 인증 주체, 권한, 트랜잭션, 도메인 조합을 처리한다.
5. Domain 객체가 상태 전이와 핵심 규칙을 검증한다.
6. Repository가 영속화를 수행한다.
7. Result를 공통 `ApiResponse`로 감싸 반환한다.

## 5. Request DTO 기준

Request DTO는 HTTP 입력 모델이 의미 있는 단위일 때만 만든다.

| 상황 | 기준 |
| --- | --- |
| 단일 `PathVariable` 조회 | DTO 없이 `@PathVariable Long id` 사용 |
| 단일 `PathVariable` 삭제 | DTO 없이 `@PathVariable Long id` 사용 |
| query parameter가 여러 개인 목록/검색 | `*ListRequest` 또는 `*SearchRequest` 사용 |
| request body가 있는 생성 | `*CreateRequest` 사용 |
| request body가 있는 수정 | `*UpdateRequest` 사용 |
| 상태 변경 | `*StatusUpdateRequest`, `*AllowStatusUpdateRequest` 사용 |

예시:

```java
@GetMapping
public ApiResponse list(@Valid @ModelAttribute HospitalListRequest query, HttpServletRequest request) {
    return ApiResponse.success(...);
}

@GetMapping("/{id}")
public ApiResponse get(@PathVariable Long id, HttpServletRequest request) {
    return ApiResponse.success(...);
}
```

snake_case query parameter는 Request DTO에서 `@BindParam`으로 처리한다.

## 6. Application 기준

Application 계층은 유스케이스의 중심이다.

- 트랜잭션 경계
- 권한 확인
- 여러 Repository 조합
- 도메인 객체 생성/수정
- 운영 히스토리 기록
- 캐시 무효화
- Result 조립

Service는 HTTP Request DTO를 직접 의존하지 않는다. Controller에서 Command/Query로 변환해 넘긴다.

## 7. Domain 기준

Domain 계층은 DB 테이블을 단순히 옮긴 폴더가 아니다.

- 엔티티
- enum
- 상태 전이 메서드
- 도메인 불변식
- 연관관계 기준

단순 조회 조합이나 화면 표시용 문자열은 domain에 넣지 않는다.

## 8. Infrastructure 기준

Infrastructure는 외부 기술 구현이다.

- Spring Data JPA repository
- Redis cache
- 파일 storage
- queue/messaging
- scheduler adapter

도메인 규칙을 infrastructure에 넣지 않는다.

## 9. 현재 Hospital 기준

Hospital 1차 구현은 다음 구조를 따른다.

```text
adapter.in.web.staff.hospital
  controller
  request

application.hospital
  HospitalStaffService
  command
  query
  result

domain.hospital
domain.account
domain.category
domain.operationhistory

infrastructure.persistence.hospital
infrastructure.persistence.account
infrastructure.persistence.category
infrastructure.persistence.operationhistory
```

검수 상태는 `PENDING`, `APPROVED`, `REJECTED`만 사용한다. 화면 표기는 신청, 승인, 반려를 기준으로 한다.

연락처는 병원 본문 컬럼에 흩뿌리지 않고 `hospital_contacts`에서 타입과 순번으로 관리한다.

## 10. 인증/인가 방향

초기 보안 설정은 public/actuator 일부만 열고 나머지는 인증 필요 기준으로 둔다. 실제 인증 방식은 계정 도메인을 구현하면서 확정한다.

- Staff API: 내부 운영자 인증과 권한 코드 확인
- Hospital API: 병원 계정 인증과 `hospital_id` 소유권 확인
- Beauty API: 뷰티 계정 인증과 `beauty_id` 소유권 확인
- User API: 일반 사용자 인증과 활성/차단 상태 확인

프론트 권한 제어는 UX 보조 수단이고, 최종 권한 검증은 백엔드가 책임진다.
