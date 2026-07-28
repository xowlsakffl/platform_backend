# Medi 백엔드 개발 문서

작성 기준: 2026-07-28

이 폴더는 Medi 백엔드의 구조, 권한, 도메인 규칙, 운영 기준을 관리한다.

세부 문서는 실제 Spring Boot 구현 기준과 맞아야 한다. 오래된 기술 스택, 경로, 클래스명이 보이면 구현 전에 문서를 먼저 교정한다.

## 문서 목록

- [아키텍처 & 흐름](./architecture.md)
- [디렉토리 구조](./directory-structure.md)
- [백엔드 구현 원칙](./implementation-principles.md)
- [API 응답 / 페이지네이션 규칙](./api-response.md)
- [에러 / 예외 처리](./error-handling.md)
- [인증 설계](./authentication.md)
- [권한 / 메뉴 설계 (Staff / Hospital / Beauty / User)](./authorization.md)
- [내부도구 허브 운영 가이드](./internal-tools.md)
- [로깅 전략 (감사로그 / 운영로그)](./logging.md)
- [Queue 운영 가이드](./queue.md)
- [Cache / Redis 적용 규칙](./cache.md)
- [Scheduler 운영 가이드](./scheduler.md)
- [도메인 & 상태 정의서](./domain-status-definition.md)
- [카테고리 설계](./category.md)
- [운영 히스토리 설계](./operation-history.md)
- [콘텐츠 신고 / 신고게시물 관리](./content-report.md)
- [채팅 설계](./chat.md)
- [알림 설계](./notification.md)
- [DBML 스키마 초안](./schema.dbml)

## 현재 기술 기준

- 백엔드는 Java 21, Spring Boot, Gradle 단일 애플리케이션으로 시작한다.
- DB는 MySQL, 마이그레이션은 Flyway를 기준으로 한다.
- 캐시와 비동기 처리 기반은 Redis를 기준으로 둔다.
- API namespace는 `/api/v1/public`, `/api/v1/user`, `/api/v1/staff`, `/api/v1/hospital`, `/api/v1/beauty`를 사용한다.
- 패키지는 Actor가 아니라 도메인 기준으로 나눈다. Actor는 URL, 인증 주체, 권한 정책의 구분값이다.
- DB 테이블명과 API 리소스 경로에는 임시 브랜드명 `medi`를 넣지 않는다.

## 현재 기준 핵심 요약

- Staff API는 대시보드, 병원/입점신청/뷰티/회원/의료진/전문가, 카테고리/해시태그, 병원 이벤트/이벤트 DB/리얼모델 DB, 동영상, 토크/후기/평가/신고, 공지/FAQ 운영을 담당한다.
- Hospital API는 병원 계정 인증, 프로필, 비밀번호, 관리자 메모, 병원 동영상 요청, 파트너 취소를 담당한다.
- Beauty API는 뷰티 계정 인증, 프로필, 비밀번호, 관리자 메모를 담당한다.
- User API는 앱 사용자 인증/프로필, 채팅, 토크/후기 작성, 신고, 병원 이벤트 DB/리얼모델 DB 신청, 사용자 차단, 알림을 담당한다.
- 공지사항/FAQ 도메인은 Staff API 기준으로 CRUD와 에디터 이미지를 지원한다.
- FAQ 카테고리는 전용 테이블이 아니라 공통 `Category` 도메인의 `FAQ` 분류를 사용한다.
- 병원/의료진/후기/영상 의료 카테고리는 `HOSPITAL_MEDICAL` 트리를 공유하고, 성형/쁘띠 구분은 `categories.group_code`, 화면별 노출 목록은 `category_usages`로 분리한다.
- Category Staff API는 관리 CRUD와 selector를 제공하고, 의료 카테고리 250개와 8개 사용처 90개 매핑은 Flyway 기준 데이터로 관리한다.
- 병원/의료진/이벤트의 `allow_status`는 `PENDING`/`APPROVED`/`REJECTED`를 저장하고, 관리자 화면 표기는 `신청`/`승인`/`반려`를 사용한다.
- 입점신청은 Staff 목록/상세/summary/승인상태 변경 API를 제공하며, 승인상태 변경은 `OperationHistory`에 기록한다.
- 토크/병의원 후기/병의원 평가는 Staff 운영 API와 User 작성 API를 Actor 기준으로 분리한다.
- 병의원 후기는 `HospitalReview`, 댓글은 `HospitalReviewComment`, 병의원 평가는 `HospitalEvaluation` 도메인이 소유한다.
- 신고 기능은 건별 로그(`ContentReport`)와 대상별 현재 상태(`ContentReportState`)를 분리한다.
- 관리자 화면 처리 이력은 `operation_histories` 부모 이력과 `operation_history_changes` 변경 상세로 분리한다.
- 권한 단일 소스는 백엔드 권한 코드와 seed/migration 데이터이며, 프론트 권한 매핑은 이를 따라간다.
- 신고게시물, 이벤트 DB, 리얼모델 DB, 입점신청처럼 같은 메뉴 그룹에 있어도 업무 책임이 다른 리소스는 별도 권한으로 분리한다.
- summary count와 빈번한 selector는 Redis 캐시 후보이며, 원본 데이터 변경 후 관련 캐시를 무효화한다.
- 모든 예외 응답은 공통 예외 핸들러/응답 포맷 규칙을 따른다.
