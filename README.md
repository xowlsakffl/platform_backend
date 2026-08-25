# K-뷰티 플랫폼 Backend

반영구, 에스테틱, 헤어, 왁싱, 타투, 네일, 마사지 등 여러 뷰티 업종의 업체와 전문가를 관리하고 사용자 탐색·상담·예약으로 확장하기 위해 개발 중인 개인 프로젝트입니다.

현재 백엔드는 **운영자 업체 관리와 파트너 온보딩을 중심으로 한 서비스 기반**을 구현했습니다. 단순 CRUD보다 Staff, Partner, User의 접근 경계, 인증 세션, 상태 변경 이력과 동시 수정 시 데이터 일관성을 먼저 설계했습니다.

## 개발 배경 및 목표

뷰티 플랫폼은 업체 기본정보만 저장한다고 끝나지 않습니다. 업종과 서비스 옵션, 전문가, 미디어, 영업시간, 사업자 정보가 함께 연결되고, 입점 신청·검수·승인·운영중지처럼 운영 상태도 지속해서 변경됩니다. 내부 운영자와 파트너가 같은 데이터를 다루더라도 허용되는 조회와 변경 범위는 달라야 합니다.

이 프로젝트는 결제나 정산부터 넓히기보다 다음 기반을 먼저 만드는 데 집중했습니다.

- Actor별 API와 인증·권한 경계
- 업체 등록, 검수, 담당자 지정과 파트너 계정 초대
- 업종별 카테고리, 서비스 옵션, 전문가와 미디어 관리
- 트랜잭션, 비관적 잠금, 변경 이력을 이용한 운영 데이터 보호
- 이후 사용자 탐색과 상담·예약 도메인을 추가할 수 있는 모듈형 구조

## 아키텍처

Spring의 일반적인 Controller-Service-Repository 흐름을 유지하면서 도메인과 외부 구현을 분리한 모듈형 모놀리스입니다.

```text
HTTP Request
  -> adapter.in.web.{staff|partner|user}
  -> application.{domain} Service
  -> domain Entity / Policy
  -> infrastructure.persistence / redis / storage
```

- `adapter.in.web`: Actor별 Controller, Request와 HTTP 응답 변환
- `application`: 유스케이스, 권한·소유권 검증, 트랜잭션과 결과 조립
- `domain`: 엔티티, 상태와 핵심 규칙
- `infrastructure`: JPA Repository, Redis, 파일 저장소
- `common`: Security, 예외, API 응답, 요청 추적과 설정

## 핵심 구현

### 1. Actor별 인증과 권한

- `/api/v1/staff`, `/partner`, `/user` 진입점 분리
- Staff·Partner는 로그인 아이디, User는 이메일 기반 로그인
- 15분 JWT Access Token과 MySQL 기반 회전형 Refresh Session
- Refresh Token 원문 대신 SHA-256 해시 저장, HttpOnly 쿠키 전달
- Access Token과 확인된 사용자 정보는 프론트 메모리에만 보관
- Redis 기반 로그인 시도 제한과 로그아웃 Access Token 폐기
- 비밀번호 재설정 토큰 단일 사용, 만료와 전체 세션 폐기
- Staff 역할·권한 검사와 Partner 리소스 소유권 검사

### 2. 업체 운영과 온보딩

- 업체 목록·상세·등록·부분 수정·soft delete
- 계정, 입점 검수와 운영 상태를 각각 분리해 변경
- 업체 담당 직원 지정·변경·해제와 일반 직원의 자기 담당 등록
- 직접 입점과 운영자 등록 경로 구분
- 일회용 파트너 계정 초대 링크와 append-only 초대 이력
- 연락처, 사업자 정보, 영업시간, 휴무 정책, 위치와 업체 특징 관리
- 검색, 다중 필터, 정렬, 페이지네이션과 운영 요약

### 3. 카테고리·옵션·전문가·미디어

- 업종 카테고리와 사용처별 selector API
- 업종별 서비스 옵션의 정상가·할인가 관리
- 전문가별 제공 옵션과 가격 override
- Staff 전문가 관리와 Partner 자기 소속 전문가 관리 API
- 업체·전문가·카테고리 미디어 저장, 교체, 조회와 삭제
- 승인·노출 조건을 충족한 전문가 프로필과 카테고리 아이콘 공개 조회

### 4. 데이터 일관성과 운영 추적

- 생성·수정·상태 변경을 Application Service의 `@Transactional` 경계에서 처리
- 업체, 사업자등록번호, 옵션, 전문가와 초대 데이터에 `PESSIMISTIC_WRITE` 적용
- 상태와 주요 필드 변경 전후 값을 운영 이력으로 저장
- 신규 미디어는 롤백 시 삭제하고 교체·삭제 파일은 DB 커밋 후 정리
- 원본 데이터 커밋 후 Redis 요약 캐시 무효화
- 공통 `ApiResponse`, Bean Validation, 예외 코드와 요청 trace ID 적용

## 현재 구현 범위

| 영역 | 상태 | 내용 |
| --- | --- | --- |
| 공통 기반 | 구현 | API 응답, 예외, 요청 추적, 페이지네이션, Flyway |
| 인증 | 구현 | Staff·Partner·User 로그인, 갱신, 로그아웃, 비밀번호 재설정 |
| 업체 운영 | 구현 | 등록·조회·수정·상태·담당자·이력·삭제 |
| 파트너 온보딩 | 구현 | 계정 초대, 초대 수락, Partner 소유권 검증 |
| 카테고리·옵션 | 구현 | 업종 분류, 업체 특징, 서비스 옵션과 가격 |
| 전문가·미디어 | 구현 | Staff/Partner 전문가 API, 미디어 수명주기 |
| 사용자 공개 조회 | 일부 구현 | 공개 미디어, 카테고리 아이콘, 전문가 프로필 |
| 이벤트·후기·채팅·알림 | 미구현 | 다음 개발 단계 |
| 상담·예약·결제·정산 | 미구현 | 도메인 확장 단계에서 검토 |

미구현 도메인의 집계값은 임의 데이터로 만들지 않고 현재 응답에서 `0`으로 반환합니다.

## 기술 스택

- Java 21
- Spring Boot 4.1
- Spring Security, OAuth2 Resource Server, JWT
- Spring Data JPA, MySQL 8, Flyway
- Redis
- Gradle
- JUnit 5, H2

## API Namespace

| Actor | Base path | 현재 역할 |
| --- | --- | --- |
| Staff | `/api/v1/staff` | 업체·카테고리·전문가 관리와 검수 |
| Partner | `/api/v1/partner` | 온보딩과 자기 업체의 옵션·전문가 관리 |
| User | `/api/v1/user` | 인증과 공개 미디어 조회 |

상세 정책과 API 범위는 [`docs/README.md`](./docs/README.md)에서 확인할 수 있습니다.

## 로컬 실행

MySQL 8과 Redis를 먼저 실행한 뒤 애플리케이션을 시작합니다.

```bash
cp .env.example .env
./gradlew bootRun
```

기본 API 주소는 `http://localhost:8081`입니다. 데이터베이스와 Redis, JWT, 쿠키, CORS, 메일 설정은 [`.env.example`](./.env.example)을 기준으로 구성합니다.

`local` 프로필에서는 화면 확인을 위한 일반 Staff 계정과 여러 상태의 가상 파트너 데이터를 선택적으로 생성할 수 있습니다. 최초 최고관리자 생성과 샘플 데이터 설정은 [인증 문서](./docs/authentication.md)와 [제품 기획 문서](./docs/platform-product-plan.md)를 참고합니다.

## 검증

```bash
./gradlew test
./gradlew build
```

현재 도메인 규칙, 애플리케이션 서비스, 트랜잭션과 주요 Staff API를 검증하는 테스트를 포함합니다.

## 주요 문서

- [아키텍처와 디렉터리](./docs/architecture.md)
- [인증과 권한](./docs/authentication.md)
- [도메인 정책과 상태](./docs/domain-status-definition.md)
- [카테고리](./docs/category.md)
- [업체 특징](./docs/partner-feature.md)
- [미디어](./docs/media.md)
- [운영 이력](./docs/operation-history.md)
- [현재 DB 스키마](./docs/schema.dbml)

## 본인 기여

서비스 요구사항과 운영 흐름 정의부터 도메인·상태·권한 정책, 데이터베이스와 API 설계, 인증·업체·카테고리·전문가·미디어 기능 구현, 테스트와 문서화까지 백엔드 전반을 단독으로 진행하고 있습니다.
