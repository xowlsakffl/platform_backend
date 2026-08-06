# platform_backend

반영구, 에스테틱, 미용실, 왁싱, 타투, 네일아트, 마사지 등을 다루는 K-뷰티 플랫폼의 Spring Boot 백엔드 단일 애플리케이션이다. 프론트엔드 저장소와 분리해 운영하며, 백엔드는 도메인 기준 패키지를 가진 모듈형 모놀리스로 개발한다.

정식 제품 운영을 목표로 업체·전문가·옵션·이벤트 탐색과 상담/예약 연결 흐름을 단계적으로 완성한다. 앱 내 결제와 정산은 현재 개발 우선순위에 포함하지 않는다.

## 기술 기준

- Java 21, Spring Boot 4, Gradle
- MySQL 8, Flyway, Spring Data JPA
- Redis, Spring Security, JWT
- 로컬 파일 저장소

## 문서

- [작업 기준](./AGENTS.md)
- [플랫폼 제품 기획](./docs/platform-product-plan.md)
- [문서 인덱스](./docs/README.md)
- [아키텍처와 디렉터리](./docs/architecture.md)
- [API 응답과 예외](./docs/api-response.md)
- [인증과 권한](./docs/authentication.md)
- [도메인 정책과 상태](./docs/domain-status-definition.md)
- [카테고리](./docs/category.md)
- [업체 특징](./docs/partner-feature.md)
- [미디어](./docs/media.md)
- [운영 이력](./docs/operation-history.md)
- [캐시](./docs/cache.md)
- [현재 DB 스키마](./docs/schema.dbml)

## 로컬 실행

Docker 없이 WSL의 MySQL과 Redis를 사용한다.

```bash
sudo service mysql start
sudo service redis-server start
cd /home/platform/platform_backend
./gradlew bootRun
```

기본 로컬 연결값은 다음과 같다.

```text
MySQL: localhost:3306 / database=platform / username=platform / password=platform
Redis: localhost:6379 / password=myStrongRedisPassword
API:   http://localhost:8081
```

환경 변수 목록은 [.env.example](./.env.example)을 따른다. 로컬 CORS 기본 허용 origin은 `localhost:4001`, `4002`, `4003`, `4004`다.

## 최초 Staff 계정

고정 관리자 비밀번호를 migration에 넣지 않는다. 최초 한 번만 다음 환경 변수를 사용한다.

```bash
STAFF_BOOTSTRAP_ENABLED=true \
STAFF_BOOTSTRAP_LOGIN_ID=platform_admin \
STAFF_BOOTSTRAP_EMAIL=admin@platform.local \
STAFF_BOOTSTRAP_PASSWORD='<강한 비밀번호>' \
./gradlew bootRun
```

기본 역할은 `platform.super_admin`이다. 계정 생성 뒤 `STAFF_BOOTSTRAP_ENABLED`를 제거하거나 `false`로 바꾼다.

## 로컬 일반 직원 샘플 계정

`local` 프로필에서는 담당 직원 지정 권한이 없는 `platform.staff` 역할의 비교용 계정을 자동 생성한다.

- 로그인 아이디: `platform_staff`
- 이메일: `staff@platform.local` (계정 복구용)
- 비밀번호: `Platform1234!`
- 역할: `platform.staff`
- 파트너 목록 조회와 미지정 파트너의 자기 담당 등록은 가능
- 다른 직원 지정·변경과 직원 선택 목록 조회는 불가

환경 변수 `STAFF_SAMPLE_BOOTSTRAP_ENABLED=false`로 생성을 끌 수 있다. 동일 이메일 계정이 이미 있으면 비밀번호는 변경하지 않고 `platform.staff` 역할만 확인한다.

## 로컬 파트너 샘플 데이터

DB 스키마와 Staff 권한 기준 데이터는 Flyway migration으로 관리한다. 카테고리와 업체 특징은 JSON 정의를 시작 시 동기화하고, 화면 개발용 파트너 데이터는 `local` 프로필의 부트스트랩에서 생성한다. 로컬에서는 기본 활성화되며 같은 파트너명, 계정 이메일·로그인 아이디 또는 사업자등록번호가 있으면 중복 생성하지 않는다.

- 가상 파트너 11곳: 연결 8곳, 미초대 1곳, 초대 발송 1곳, 초대 만료 1곳
- 신청·승인·반려 승인상태와 정상·운영중지·탈퇴 운영상태
- 연락처, 사업자 정보, 파트너 특징, 1뎁스 파트너 분류
- 연결 계정: `partner01@platform.local` ~ `partner08@platform.local`
- 연결 계정 로그인 아이디: `platform_partner_01` ~ `platform_partner_08`
- 초대 대상 이메일: `partner09@platform.local` ~ `partner11@platform.local`
- 공통 비밀번호: `Platform1234!`
- 미디어 파일은 생성하지 않음

환경 변수로 실행 여부와 공통 비밀번호를 바꿀 수 있다.

```bash
PARTNER_SAMPLE_BOOTSTRAP_ENABLED=true \
PARTNER_SAMPLE_BOOTSTRAP_PASSWORD='새 비밀번호' \
./gradlew bootRun
```

샘플이 이미 생성된 뒤 비밀번호 설정만 바꿔도 기존 계정 비밀번호는 변경하지 않는다. 다시 생성하려면 해당 샘플 데이터를 직접 정리해야 한다.

## 현재 구현 범위

- 공통 API 응답, 페이지네이션, 예외 처리, 요청 추적
- Staff·Partner 로그인 아이디 로그인, User 이메일 로그인, 내 정보·현재/전체 로그아웃
- 15분 JWT 액세스 토큰과 MySQL 기반 회전형 리프레시 세션
- HttpOnly 보안 쿠키, Redis 로그인 제한·로그아웃 토큰 폐기
- 세 Actor 비밀번호 찾기·일회용 재설정 링크·변경 후 전체 세션 폐기
- Staff 역할·권한 검사와 Partner 소유권 검사
- Staff용 Partner 목록·상세·등록·부분수정·승인·운영상태 변경·삭제·이력·요약
- 파트너별 내부 담당 직원 지정·변경·해제, 일반 직원 자기 담당 등록·해제
- 파트너 연락처, 사업자 정보, 특징 17개, 상세 영업시간·휴무 정책
- 직접 입점과 내부관리자 업체 등록경로, append-only 파트너 계정 초대 이력·일회용 초대 링크
- Category Staff 관리 API와 사용처 selector
- PARTNER 1뎁스 파트너 분류와 업종별 옵션 분류 기준 데이터
- 업체 옵션 정상가·할인가와 전문가별 제공 여부·가격 오버라이드
- Specialist Staff 관리 API와 Partner 자기 스페셜리스트 관리 API
- Partner, Category, Specialist 미디어 저장·교체·조회·삭제 연동
- 앱 공개용 Category 아이콘과 승인·노출 Specialist 프로필 조회

이벤트, 후기, 평가 작성, 채팅, 알림, 신고, 큐, 스케줄러는 아직 구현 범위가 아니다. 현재 Partner·Specialist 응답의 이벤트/후기 집계값은 관련 도메인이 구현될 때까지 `0`이다.

## 검증 기준

변경 범위에 맞는 단위·통합 테스트를 추가하고 다음 순서로 검증한다.

```bash
./gradlew test
./gradlew build
./gradlew bootRun
```
