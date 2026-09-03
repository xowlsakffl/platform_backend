# K-뷰티 플랫폼 Backend

반영구, 에스테틱, 헤어, 왁싱, 타투, 네일, 마사지 등 여러 업종을 다루는 사업화 목적의 지역 기반 K-뷰티 업체 탐색 플랫폼입니다. 사용자가 지역과 업종으로 업체를 찾고 시술·가격·전문가·작업 이미지와 이벤트를 비교한 뒤 신청·상담·예약까지 이어지는 구조를 목표로 합니다.

현재 백엔드는 검색 결과의 공급 데이터를 확보하고 운영할 수 있도록 내부 운영자 업체관리와 파트너 온보딩, 업체별 전문가 관리 기반을 우선 구현했습니다.

## 서비스 흐름

```text
사용자
  지역·업종 검색 -> 업체·서비스·전문가·이벤트 비교 -> 신청·상담 -> 예약

파트너 업체
  계정 가입 -> 업체 등록·선택 -> 업체·옵션·전문가·미디어 관리 -> 신청·예약 대응

플랫폼 운영자
  업체 검수 -> 담당자 지정 -> 상태·정보 품질 관리 -> 필요 시 소유 계정 변경
```

결제·정산부터 넓히지 않고 Actor별 인증과 권한, 업체·옵션·전문가·미디어, 검수와 상태 이력처럼 이후 탐색·상담·예약 도메인이 의존할 기반을 먼저 구축합니다.

## 아키텍처

Controller-Service-Repository 흐름을 유지하면서 도메인과 외부 구현을 분리한 모듈형 모놀리스입니다.

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

### 인증과 권한

- `/api/v1/staff`, `/partner`, `/user` namespace 분리
- Staff·Partner는 로그인 아이디, User는 이메일 로그인
- 15분 JWT Access Token과 MySQL 기반 회전형 Refresh Session
- Refresh Token SHA-256 해시 저장과 HttpOnly 쿠키 전달
- Redis 로그인 시도 제한과 로그아웃 Access Token 폐기
- 비밀번호 재설정 토큰 단일 사용과 전체 세션 폐기
- Staff 역할·권한과 Partner 소유권 검사

### 업체 운영과 온보딩

- 업체 목록·상세·섹션별 수정·soft delete
- 계정상태, 검수상태와 운영상태 분리
- 검수신청·검수중·검수승인·검수반려 전이 정책
- 탈퇴를 일반 운영상태 변경에서 제외
- 담당 직원 지정·변경·해제와 일반 직원 자기 담당 등록
- 계정과 업체를 멤버십으로 분리해 한 계정에서 여러 업체 관리
- 모든 업체 생성 시 등록 계정의 활성 OWNER 멤버십을 함께 생성
- Staff가 기존 활성 파트너 계정으로 업체 소유권 변경
- 공개 파트너 포털에서 공지사항·FAQ를 제공하고 로그인 후 내 업체·신규 등록 제공
- 업체가 2개 이상일 때 관리자 헤더에서 업체 전환
- 연락처, 사업자 정보, 운영시간, 휴무, 위치와 업체 특징 관리
- 검색, 다중 필터, 정렬과 페이지네이션

### 카테고리·옵션·전문가·미디어

- JSON 정의 기준 카테고리와 업체 특징 시작 시 동기화
- 업체 1뎁스 분류와 업종별 2뎁스 옵션 카테고리
- 업체 옵션 정상가·할인가와 전문가별 가격 override
- 업체 상세 하위 Staff 전문가 관리와 Partner 자기 소속 관리 API
- 전문가 순서 변경, 검수·노출상태와 업체 범위 내 근무시간 검증
- 전문가별 휴무 정책
- 프로필 이미지 최대 3장과 비공개 인증 이미지 최대 5장
- 업체·전문가·카테고리 미디어 저장, 교체, 조회와 삭제

### 데이터 일관성과 추적

- Application Service의 `@Transactional` 경계
- 주요 수정 대상의 `PESSIMISTIC_WRITE`
- 상태와 주요 필드 변경 전후 운영 이력
- 롤백 시 신규 파일 삭제와 커밋 후 교체 파일 정리
- 원본 커밋 후 Redis 캐시 무효화
- 공통 `ApiResponse`, Bean Validation, 예외 코드와 trace ID

## 현재 구현 범위

| 영역 | 상태 | 내용 |
|---|---|---|
| 공통 기반 | 구현 | API 응답, 예외, 요청 추적, 페이지네이션, Flyway |
| 인증 | 구현 | Staff·Partner·User 로그인, 갱신, 로그아웃, 비밀번호 재설정 |
| 업체 운영 | 구현 | 조회·수정·상태·담당자·소유 계정 변경·이력·삭제 |
| 파트너 온보딩 | 구현 | 계정 선가입, 다중 업체 등록·선택, 입점 임시저장·제출, 멤버십 소유권 검증 |
| 카테고리·옵션 | 구현 | 업종 분류, 업체 특징, 서비스 옵션과 가격 |
| 전문가·미디어 | 구현 | Staff/Partner 전문가 API와 미디어 수명주기 |
| 사용자 공개 조회 | 일부 구현 | 공개 미디어, 카테고리 아이콘, 전문가 프로필 |
| 이벤트·신청 | 미구현 | 업체별 프로모션과 사용자 신청 |
| 상담·예약 | 미구현 | 신청 이후 상담과 일정 확정 |
| 후기·채팅·알림 | 미구현 | 이용 경험과 커뮤니케이션 |
| 결제·정산 | 미구현 | 예약 흐름 안정화 이후 검토 |

미구현 집계는 운영 지표로 해석하지 않으며 관련 도메인이 완성될 때까지 응답에서 `0`으로 반환합니다.

## 기술 스택

- Java 21, Spring Boot 4.1, Gradle
- Spring Security, OAuth2 Resource Server, JWT
- Spring Data JPA, MySQL 8, Flyway
- Redis
- JUnit 5, H2

## API Namespace

| Actor | Base path | 현재 역할 |
|---|---|---|
| Staff | `/api/v1/staff` | 업체·카테고리·전문가 관리와 검수 |
| Partner | `/api/v1/partner` | 계정 가입, 파트너 포털과 업체별 온보딩·옵션·전문가 관리 |
| User | `/api/v1/user` | 인증과 공개 미디어 조회 |

## 로컬 실행

MySQL 8과 Redis를 실행하고 환경변수를 설정한 뒤 시작합니다.

```bash
cp .env.example .env
./gradlew bootRun
```

기본 API 주소는 `http://localhost:8081`입니다. 데이터베이스, Redis, JWT, 쿠키, CORS와 메일 설정은 [`.env.example`](./.env.example)을 기준으로 구성합니다.

사업자등록증 OCR은 네이버 클라우드 CLOVA OCR의 Document `사업자등록증(KR)` 모델을 사용합니다. Document 도메인의 API Gateway 연동 화면에서 발급한 값을 `.env`에 설정하고 백엔드를 재시작합니다. 네이버 클라우드 계정의 Access Key/Secret Key가 아니라 OCR 도메인의 APIGW Invoke URL과 `X-OCR-SECRET` 값이어야 합니다.

```dotenv
BUSINESS_REGISTRATION_OCR_ENABLED=true
BUSINESS_REGISTRATION_OCR_INVOKE_URL=https://example.apigw.ntruss.com/...
BUSINESS_REGISTRATION_OCR_SECRET=replace-with-domain-x-ocr-secret
BUSINESS_REGISTRATION_OCR_CONFIRMATION_CONFIDENCE=0.8
```

최초 최고관리자 생성과 local 프로필의 일반 Staff·상태별 파트너 샘플 데이터는 [인증 문서](./docs/authentication.md)와 [제품 기획 문서](./docs/platform-product-plan.md)를 참고합니다. 고정 관리자 비밀번호는 migration에 저장하지 않습니다.

## 검증

```bash
./gradlew clean test
./gradlew build
```

도메인 상태 정책, 운영시간 범위, 전문가 순서 변경, 트랜잭션과 주요 Staff API 테스트를 포함합니다.

## 주요 문서

- [문서 인덱스](./docs/README.md)
- [제품 기획](./docs/platform-product-plan.md)
- [아키텍처와 디렉터리](./docs/architecture.md)
- [인증과 권한](./docs/authentication.md)
- [도메인 정책과 상태](./docs/domain-status-definition.md)
- [카테고리](./docs/category.md)
- [업체 특징](./docs/partner-feature.md)
- [미디어](./docs/media.md)
- [운영 이력](./docs/operation-history.md)
- [현재 DB 스키마](./docs/schema.dbml)

## 본인 기여

서비스 요구사항과 운영 흐름 정의부터 도메인·상태·권한 정책, 데이터베이스와 API 설계, 인증·업체·카테고리·전문가·미디어 기능 구현, 테스트와 문서화까지 백엔드 전반을 구현하고 있습니다.
