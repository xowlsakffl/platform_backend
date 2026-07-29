# medi_backend

성형·뷰티 플랫폼의 Spring Boot 백엔드 단일 애플리케이션이다. 프론트엔드 저장소와 분리해 운영하며, 백엔드는 도메인 기준 패키지를 가진 모듈형 모놀리스로 개발한다.

## 기술 기준

- Java 21, Spring Boot 4, Gradle
- MySQL 8, Flyway, Spring Data JPA
- Redis, Spring Security, JWT
- 로컬 파일 저장소

## 문서

- [작업 기준](./AGENTS.md)
- [문서 인덱스](./docs/README.md)
- [아키텍처와 디렉터리](./docs/architecture.md)
- [API 응답과 예외](./docs/api-response.md)
- [인증과 권한](./docs/authentication.md)
- [도메인 정책과 상태](./docs/domain-status-definition.md)
- [카테고리](./docs/category.md)
- [미디어](./docs/media.md)
- [운영 이력](./docs/operation-history.md)
- [캐시](./docs/cache.md)
- [현재 DB 스키마](./docs/schema.dbml)

## 로컬 실행

Docker 없이 WSL의 MySQL과 Redis를 사용한다.

```bash
sudo service mysql start
sudo service redis-server start
cd /home/medi/medi_backend
./gradlew bootRun
```

기본 로컬 연결값은 다음과 같다.

```text
MySQL: localhost:3306 / database=medi / username=medi / password=medi
Redis: localhost:6379 / password=myStrongRedisPassword
API:   http://localhost:8080
```

환경 변수 목록은 [.env.example](./.env.example)을 따른다. 로컬 CORS 기본 허용 origin은 `localhost:3000`, `3001`, `3002`다.

## 최초 Staff 계정

고정 관리자 비밀번호를 migration에 넣지 않는다. 최초 한 번만 다음 환경 변수를 사용한다.

```bash
STAFF_BOOTSTRAP_ENABLED=true \
STAFF_BOOTSTRAP_EMAIL=admin@medi.local \
STAFF_BOOTSTRAP_PASSWORD='<강한 비밀번호>' \
./gradlew bootRun
```

기본 역할은 `platform.super_admin`이다. 계정 생성 뒤 `STAFF_BOOTSTRAP_ENABLED`를 제거하거나 `false`로 바꾼다.

## 현재 구현 범위

- 공통 API 응답, 페이지네이션, 예외 처리, 요청 추적
- Staff, Hospital, Beauty, User 이메일 로그인·내 정보·로그아웃
- JWT 인증과 Redis 기반 로그아웃 토큰 폐기
- Staff 역할·권한 검사와 Hospital 소유권 검사
- Hospital Staff 목록·상세·등록·부분수정·상태·검수·삭제·이력·요약
- 병원 연락처, 사업자 정보, 특징 12개, 통역 가능 언어 5개
- Category Staff 관리 API와 사용처 selector
- Hospital Medical 3단계 카테고리 기준 데이터
- Doctor Staff 관리 API와 Hospital 자기 병원 관리 API
- Hospital, Category, Doctor 미디어 저장·교체·조회·삭제 연동
- 앱 공개용 Category 아이콘과 승인·노출 Doctor 프로필 조회

이벤트, 후기, 평가 작성, 채팅, 알림, 신고, 큐, 스케줄러는 아직 구현 범위가 아니다. 현재 Hospital·Doctor 응답의 이벤트/후기 집계값은 관련 도메인 이식 전까지 `0`이다.

## 검증 기준

현재 단계에서는 테스트 코드를 추가하지 않는다. 변경 후 다음 순서로 검증한다.

```bash
./gradlew compileJava
./gradlew build -x test
./gradlew bootRun
```
