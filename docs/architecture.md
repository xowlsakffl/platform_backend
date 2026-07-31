# 아키텍처와 디렉터리

## 구조 선택

백엔드는 단일 배포 단위의 모듈형 모놀리스다. Spring의 일반적인 `Controller-Service-Repository` 흐름을 유지하면서 도메인 모델과 외부 연동을 분리한다.

```text
HTTP
  -> adapter.in.web.{actor}.controller
  -> adapter.in.web.{actor}.request
  -> application.{domain}.Service
  -> domain.{domain}
  -> infrastructure.persistence / redis / storage
```

별도 `Action` 클래스를 기계적으로 만들지 않는다. 유스케이스가 작으면 application Service 메서드가 진입점이며, 공통 쓰기·조회 조립이 커지면 `SpecialistWriteService`, `SpecialistResultAssembler`처럼 책임 단위로 분리한다.

## 패키지 구조

```text
src/main/java/com/medi/
  adapter/in/web/
    staff/       내부 운영자 API
    partner/    파트너 파트너 API
    beauty/      뷰티 파트너 API
    user/        사용자 앱 API
  application/
    auth/        인증·권한 유스케이스
    cache/       캐시 포트와 무효화
    category/    카테고리 유스케이스, command/query/result
    specialist/      스페셜리스트 유스케이스, command/query/result
    partner/    파트너 유스케이스, command/query/result
    media/       미디어 정책, 저장 포트, 응답
  domain/
    account/
    auth/       Actor 공통 인증 세션
    category/
    specialist/
    partner/
    media/
    operationhistory/
  infrastructure/
    persistence/ JPA Repository
    redis/       Redis 구현
    storage/     로컬 파일 저장소 구현
  common/
    config/      Security, CORS 등 전역 설정
    error/       공통 예외와 핸들러
    security/    JWT, 쿠키 요청 보호, 인증 주체와 설정
    web/         API 응답, trace, 인증 web 지원, multipart 공통
src/main/resources/
  db/migration/  Flyway migration
```

`adapter.in.web`의 최상위에는 네 Actor 폴더만 둔다. 도메인 공통 HTTP 진입점도 실제 호출 Actor 아래에 둔다.

## 계층 책임

### Controller와 Request

- URL, HTTP method, content type을 선언한다.
- `@ModelAttribute` 또는 `@RequestBody` Request를 검증한다.
- Request를 Command/Query로 바꾸고 application Service를 호출한다.
- 결과를 공통 `ApiResponse`로 포장한다.

### Application

- 권한과 소유권을 확인한다.
- 트랜잭션 경계를 정한다.
- Repository, 도메인 객체, 미디어, 이력, 캐시를 조합한다.
- HTTP 타입에 의존하지 않는다.

### Domain

- JPA 엔티티와 enum을 소유한다.
- 상태 변경과 soft delete 같은 핵심 규칙을 메서드로 표현한다.
- Controller나 Redis, 파일 시스템을 참조하지 않는다.

### Infrastructure

- Spring Data JPA Repository를 제공한다.
- Redis와 파일 저장소 포트를 구현한다.
- 비즈니스 상태를 새로 결정하지 않는다.

## Actor와 도메인

Actor 분리는 같은 엔티티를 중복 구현한다는 뜻이 아니다. Specialist 엔티티는 하나지만 Staff와 Partner의 요청, 권한, 응답 공개 범위가 다르므로 Controller와 application 유스케이스를 분리한다.

| Actor | 현재 역할 |
|---|---|
| Staff | 파트너·카테고리·스페셜리스트 관리, 검수, 증빙 조회 |
| Partner | 자기 파트너의 스페셜리스트 관리와 증빙 조회 |
| Beauty | 이메일 인증 기반만 구현 |
| User | 이메일 인증과 공개 미디어 조회만 구현 |

## 트랜잭션 기준

- 생성·수정·상태 변경·삭제는 application Service에서 `@Transactional`을 사용한다.
- 목록·상세는 `@Transactional(readOnly = true)`를 사용한다.
- 동시 수정에 민감한 파트너·스페셜리스트·카테고리는 쓰기 전에 비관적 잠금을 사용한다.
- 새 미디어 파일은 트랜잭션 롤백 시 정리하고, 교체·삭제 파일은 커밋 후 정리한다.
- 캐시 무효화는 원본 DB 커밋 뒤 실행한다.
