# Medi Backend Architecture

작성 기준: 2026-07-28

이 문서는 Medi 백엔드의 현재 구조 기준이다. Beaulab 문서에서 가져온 도메인/상태/운영 규칙은 유지하되, 구현 구조는 Java 21 + Spring Boot 단일 애플리케이션 기준으로 다시 잡는다.

## 1) API 엔드포인트 구성

API는 v1 아래에서 Actor namespace로 분기한다.

- Public API: `/api/v1/public/*`
- Staff API: `/api/v1/staff/*`
- Hospital API: `/api/v1/hospital/*`
- Beauty API: `/api/v1/beauty/*`
- User API: `/api/v1/user/*`

Staff 프론트 메뉴 prefix와 API path는 반드시 같을 필요가 없다. API는 리소스 소유권 기준으로 `hospital-entries`, `hospital-event-dbs`, `hospital-event-real-model-dbs`처럼 도메인 리소스명을 유지한다.

## 2) 패키지 구조 원칙

패키지는 Actor 기준이 아니라 도메인 기준으로 나눈다. Actor는 URL prefix, 인증 주체, 권한 정책을 구분하는 값이다.

```text
src/main/java/com/medi/
  common/
    config/
    error/
    security/
    web/
  domain/
    account/
    hospital/
    beauty/
    category/
    media/
    operationhistory/
    content/
    notification/
  infrastructure/
    persistence/
    redis/
    storage/
    messaging/
```

원칙:

- Controller는 각 도메인의 inbound adapter로 둔다.
- Application service/use case는 트랜잭션, 권한 확인, 여러 도메인 조합, 운영 히스토리 기록 조합을 담당한다.
- Domain model은 상태 전이와 핵심 불변식을 가진다.
- Repository interface는 도메인 또는 application 계층에 두고, JPA 구현은 infrastructure에 둔다.
- DTO/response mapper는 응답 구조 변환만 담당한다. mapper에서 새 DB 조회를 만들지 않는다.
- 상태 라벨은 프론트에서 임의로 만들지 않고 백엔드 응답에 함께 내려준다.
- 운영 히스토리는 도메인 변경 전/후 스냅샷을 application 계층에서 조립하고 공통 이력 저장 유스케이스에 맡긴다.

## 3) 요청 처리 흐름

1. Controller에서 요청 바인딩과 Bean Validation 수행
2. Security/permission policy로 인증과 인가 확인
3. Application service/use case에서 트랜잭션과 비즈니스 흐름 처리
4. Domain model에서 상태 전이와 불변식 검증
5. Repository/JPA로 영속화
6. 공통 API 응답 포맷으로 반환

## 4) 인증/인가 흐름

초기 보안 설정은 public/actuator 일부만 열고 나머지는 인증 필요 기준으로 둔다. 실제 인증 방식은 사용자/파트너/관리자 계정 모델을 구현하면서 확정한다.

Actor별 보호 API 기준:

- Staff 보호 API
  - 인증된 staff 계정
  - staff 권한 코드 확인
  - `common.access` 성격의 관리자 접근 권한 확인
- Hospital 보호 API
  - 인증된 hospital 계정
  - 로그인 계정의 `hospital_id` 기반 소유권 확인
- Beauty 보호 API
  - 인증된 beauty 계정
  - 로그인 계정의 `beauty_id` 기반 소유권 확인
- User 보호 API
  - 인증된 user 계정
  - 활성/차단 상태 확인

비밀번호 재설정 API는 로그인 전에도 호출되어야 하므로 보호 API 밖에 둔다. 대신 rate limit key를 분리한다.

- 로그인: `auth-login`
- 비밀번호 재설정 링크 발송: `password-reset-link`
- 비밀번호 재설정 토큰 검증: `password-reset-verify`
- 비밀번호 재설정 제출: `password-reset-submit`
- 로그인 후 비밀번호 변경: `password-update`
- 사용자 신고 생성: `content-report-create`

## 5) 현재 주요 도메인

- 계정: `AccountStaff`, `AccountHospital`, `AccountBeauty`, `AccountUser`, `AccountUserAccessLog`, `AccountUserBlock`
- 파트너: `Hospital`, `HospitalEntry`, `Beauty`, `HospitalDoctor`, `BeautyExpert`, `HospitalFeature`
- 병원 이벤트/광고/고객 DB: `HospitalEvent`, `HospitalEventAd`, `HospitalEventDB`, `HospitalEventRealModelDB`, `HospitalEventOption`, `HospitalEventDoctorAssignment`
- 콘텐츠: `Talk`, `TalkComment`, `TalkCommentMention`, `TalkPoll`, `TalkPollOption`, `TalkPollVote`, `TalkSave`, `HospitalReview`, `HospitalReviewComment`, `HospitalReviewCommentMention`, `HospitalEvaluation`, `HospitalVideo`, `Notice`, `Faq`
- 커뮤니케이션: `Chat`, `ChatMessage`, `ChatParticipant`, `NotificationInbox`, `NotificationDelivery`, `NotificationDevice`, `NotificationPreference`
- 공통 운영: `Media`, `Category`, `CategoryUsage`, `Hashtag`, `AdminNote`, `ContentReport`, `ContentReportItem`, `ContentReportState`, `OperationHistory`, `OperationHistoryChange`, `PasswordReset`

파트너 계정 관계:

- `AccountHospital`과 `Hospital`은 1:1이다. 병원 계정은 `account_hospitals.hospital_id` unique 제약으로 병원당 하나만 존재한다.
- `AccountBeauty`와 `Beauty`는 1:1이다. 뷰티 계정은 `account_beauties.beauty_id` unique 제약으로 뷰티 업체당 하나만 존재한다.
- Staff 상세 응답은 복수 계정 배열을 쓰지 않고 `account_hospital`, `account_beauty` 단일 객체를 사용한다.
- Hospital/Beauty Actor API는 로그인 계정의 `hospital_id`, `beauty_id`를 소유권 기준으로 사용한다.

## 5.1) Staff 메뉴 N 배지

Staff 메뉴의 `N` 표시는 개인별 읽음/미읽음이 아니라 전역 처리대기 신호다.

- API: `GET /api/v1/staff/navigation-badges`
- 도메인: `app/Domains/Common/NavigationBadge`
- 응답 key는 프론트 메뉴 path를 사용한다. 예: `/hospital-manage/hospitals`
- 권한이 없는 메뉴의 badge는 API 응답에서 제외한다.
- 부모 메뉴 `N`은 자식 메뉴 중 하나라도 `has_new=true`이면 프론트에서 자동 표시한다.
- 광고 현황처럼 직접 처리 액션이 아닌 조회성 메뉴는 기본적으로 `N` 대상에서 제외한다.

현재 카운트 기준:

- 병원/의료진/입점신청/이벤트/광고: `allow_status = PENDING`
- 이벤트 DB: `status = NEW`
- 리얼모델 DB: `status = RECEIVED`
- 신고게시물: `ContentReportState.report_status = REPORTED`
- 성형후기/시술후기 메뉴는 게시글과 댓글 신고를 합산한다.
- 토크 메뉴는 토크와 토크 댓글 신고를 합산한다.

## 5.2) 카테고리 구조

카테고리는 공통 `categories` 테이블과 `category_usages` 테이블을 사용한다.

- `Category.domain`은 카테고리의 기본 업무 도메인을 나타낸다.
- `Category.group_code`는 같은 domain 안에서 성형/쁘띠처럼 화면과 상품 정책상 구분이 필요한 그룹을 나타낸다.
- `CategoryUsage.usage`는 같은 카테고리를 어떤 기능에서 선택지로 노출할지 결정한다.
- 병원/의료진/동영상 진료과목은 `HOSPITAL_MEDICAL` domain을 사용하되 `group_code`로 성형/쁘띠를 구분해 보여준다.
- 이벤트 광고 카테고리별 배너는 `hospital_event_ad_surgery`, `hospital_event_ad_treatment` usage로 성형/쁘띠 선택지를 분리한다.
- 카테고리 연결은 `category_assignments` polymorphic pivot을 사용한다.

## 5.3) Staff Summary Cache

Staff 목록 상단 summary는 `StaffSummaryCache`를 통해 캐시한다.

- 기본 store는 `cache.staff_summary.store`이며 기본값은 `redis`다.
- 기본 TTL은 300초다.
- Redis/cache 장애가 있어도 조회와 쓰기는 막지 않는다. 캐시는 가속 계층이고 기준 데이터는 DB다.
- summary에 영향을 주는 생성/수정/삭제/상태 변경 Action에서 `StaffSummaryCache::forget()`으로 무효화한다.

## 6) 공지사항(Notice) / FAQ 구조

현재 Notice / FAQ는 Staff API 기준으로 구현되어 있다.

- 라우트: `app/Modules/Staff/routes/api_staff.php`
- 컨트롤러:
  - `app/Modules/Staff/Http/Controllers/Notice/NoticeForStaffController.php`
  - `app/Modules/Staff/Http/Controllers/Faq/FaqForStaffController.php`
- 도메인:
  - `app/Domains/Notice/*`
  - `app/Domains/Faq/*`
  - FAQ 카테고리는 공통 `Category` 도메인의 `FAQ` 분류 사용

기능 범위:

1. 공지 CRUD
2. 채널/상태/상단고정/게시기간
3. 첨부파일 업로드
4. 에디터 이미지 업로드/정리
5. 관리자 메인 팝업(`is_important`)
6. FAQ CRUD
7. FAQ 에디터 이미지 업로드/정리

## 7) 병의원 게시물 운영 구조

병의원 게시물 계열은 Actor 진입점과 Domain 로직을 분리한다.

- Staff 라우트
  - `GET /api/v1/staff/hospital-reviews/surgery`
  - `GET /api/v1/staff/hospital-reviews/treatment`
  - `GET /api/v1/staff/hospital-review-comments`
  - `GET /api/v1/staff/hospital-evaluations`
  - `GET /api/v1/staff/reported-contents/talks`
  - `GET /api/v1/staff/reported-contents/talk-comments`
  - `GET /api/v1/staff/reported-contents/hospital-reviews/surgery`
  - `GET /api/v1/staff/reported-contents/hospital-reviews/treatment`
  - `GET /api/v1/staff/reported-contents/hospital-review-comments/surgery`
  - `GET /api/v1/staff/reported-contents/hospital-review-comments/treatment`
  - `GET /api/v1/staff/reported-contents/hospital-evaluations`
  - `GET /api/v1/staff/reported-contents/videos`
  - `GET /api/v1/staff/reported-contents/detail/{targetType}/{targetId}`
  - `GET /api/v1/staff/reported-contents/{targetType}/{targetId}/reports`
  - `PATCH /api/v1/staff/hospital-reviews/status`
  - `PATCH /api/v1/staff/hospital-review-comments/status`
  - `PATCH /api/v1/staff/hospital-evaluations/status`
  - `PATCH /api/v1/staff/hospital-evaluations/{hospitalEvaluation}/receipt/verify`
  - `PATCH /api/v1/staff/hospital-evaluations/{hospitalEvaluation}/receipt/reject`
  - `PATCH /api/v1/staff/reported-contents/status`
  - `PATCH /api/v1/staff/reported-contents/warning-status`
- User 라우트
  - `POST /api/v1/user/hospital-reviews`
  - `DELETE /api/v1/user/hospital-reviews/{hospitalReview}`
  - `POST /api/v1/user/hospital-reviews/{hospitalReview}/comments`
  - `DELETE /api/v1/user/hospital-reviews/{hospitalReview}/comments/{comment}`
  - `POST /api/v1/user/talks/{talk}/reports`
  - `POST /api/v1/user/talks/{talk}/comments/{comment}/reports`
  - `POST /api/v1/user/hospital-reviews/{hospitalReview}/reports`
  - `POST /api/v1/user/hospital-reviews/{hospitalReview}/comments/{comment}/reports`
  - `POST /api/v1/user/hospital-evaluations/{hospitalEvaluation}/reports`
  - `POST /api/v1/user/videos/{video}/reports`

도메인 책임:

- `HospitalReview`: 성형후기/시술후기 게시글, 병원/의료진/카테고리/전후 이미지/평점/비용/베스트/통계
- `HospitalReviewComment`: 후기 댓글/대댓글, 멘션, 노출상태, 게시상태, 처리 이력
- `HospitalEvaluation`: 병의원 평가, 병원/의료진/카테고리, 평가 항목, 영수증 이미지/인증/부적합 사유, 처리 이력
- `Talk`: 토크 게시글, 카테고리, 이미지, 투표, 통계, 처리 이력
- `TalkComment`: 토크 댓글/대댓글, 멘션, 노출상태, 게시상태, 처리 이력
- `HospitalVideo`: 병원 동영상, 병원/의료진/카테고리/해시태그/썸네일/유튜브 링크/공개여부/강제중지/신고 상태
- `ContentReport`: 사용자 신고 건별 로그
- `ContentReportState`: 신고 대상별 현재 신고 상태, 신고 수, 경고/무시 상태

DTO 응답 원칙:

- `author`, `hospital`, `doctor`, `category`, `parent`, `receipt`처럼 연관 객체는 개별 id/name 필드로 흩뿌리지 않고 객체로 내려준다.
- 카테고리가 여러 개인 도메인은 `categories` 배열로 내려준다.
- 목록 DTO는 eager loaded relation을 기준으로 만들고, 상세 DTO처럼 별도 조합 로직이 긴 경우 private resolver로 분리한다.

신고 상태 원칙:

- 일반 콘텐츠의 실제 노출 여부는 각 도메인 모델의 `status`로 관리한다.
- 신고 접수/자동차단/노출중지/정상노출 상태는 `ContentReportState.report_status`로 분리 관리한다.
- `AUTO_BLOCKED`, `ADMIN_HIDDEN` 상태인 콘텐츠는 일반 게시물관리에서 노출/미노출 변경이 잠긴다.
- 신고 상태 변경, 경고/무시 처리는 operation history에 기록한다.

운영 히스토리 원칙:

- 관리자 화면에 표시할 처리 이력은 `operation_histories`에 부모 이력으로 저장한다.
- 변경 필드별 전/후 값은 `operation_history_changes`에 저장한다.
- 단건 변경도 change 1건으로 저장하고, 다중 변경은 부모 이력 1건에 여러 change를 붙인다.
- 도메인 Action은 `OperationHistoryChangeSetBuilder`로 변경 payload를 만들고, 저장은 `OperationHistoryCreateAction`에 맡긴다.
- 상세 구조는 `./operation-history.md`를 따른다.

상태/검수 상태 표기 원칙:

- `status`는 실제 운영/노출 상태다.
- `allow_status`는 검수/승인 흐름이다.
- 병원/의료진/이벤트/입점신청/광고 `allow_status` 화면 표기는 모델 라벨 기준으로 `신청`/`검수`/`승인`/`반려`를 사용한다.
- 상태 전용 변경 이력은 `OperationHistory::ACTION_STATE_UPDATED`를 사용하고, 변경 필드는 실제 상태 컬럼명으로 구분한다. 예: `status`, `allow_status`, `admin_status`, `hospital_status`, `receipt_status`, `report_status`, `warning_status`.

## 7.1) 이벤트 광고 구조

이벤트 광고는 `HospitalEventAd` 도메인에서 관리한다.

- Staff API:
  - `GET /api/v1/staff/hospital-event-ads`
  - `GET /api/v1/staff/hospital-event-ads/placements`
  - `GET /api/v1/staff/hospital-event-ads/availability`
  - `GET /api/v1/staff/hospital-event-ads/calendar`
  - `GET /api/v1/staff/hospital-event-ads/{hospitalEventAd}`
  - `POST /api/v1/staff/hospital-event-ads`
  - `POST|PUT|PATCH /api/v1/staff/hospital-event-ads/{hospitalEventAd}`
  - `PATCH /api/v1/staff/hospital-event-ads/allow-status`
  - `GET /api/v1/staff/hospital-event-ads/{hospitalEventAd}/operation-histories`
- 광고 위치는 `HospitalEventAd::placements()`와 `placementGroups()`가 기준이다.
- 성형/쁘띠 카테고리별 배너만 카테고리 선택이 필수다.
- 각 위치/주차/카테고리 조합의 구좌 제한은 `HospitalEventAd::WEEKLY_SLOT_LIMIT = 3`이다.
- 광고 기간은 `HospitalEventAdPeriodResolver`가 계산한다.
- 시작 요일은 대부분 화요일이며, 쁘띠 이벤트 광고는 목요일이다.
- 판매 마감 판단은 `HospitalEventAdSalesDeadline`에 둔다.
- 광고 상태(`광고예정`/`광고중`/`광고종료`)는 저장 컬럼이 아니라 `allow_status = APPROVED`와 `start_at/end_at` 기준으로 계산한다.
- 승인 시점에는 이벤트/병원 상태, 이미지, 구좌를 다시 검증한다.

## 8) API 응답 / 페이지네이션 원칙

`LengthAwarePaginator` 기반 목록은 `App\Common\Support\PaginatedResponse`를 사용한다.

- `fromPaginator($paginator, $mapper)`: `items`와 `meta.current_page/per_page/total/last_page`를 만든다.
- `fromPaginator($paginator, $mapper, $extraMeta)`: 신고게시물 요약처럼 추가 meta가 필요할 때 사용한다.
- 상세 보조 목록도 paginator를 만든 뒤 `fromPaginator()`에 태운다. 페이지 보정이 필요하면 해당 화면/액션 정책으로 명시적으로 처리한다.

예외:

- `ChatMessageListForUserQuery`는 cursor pagination을 사용하므로 `current_page/total` 기반 `PaginatedResponse`를 사용하지 않는다.
- 파일 다운로드/스트리밍 다운로드는 JSON `ApiResponse`가 아니라 `StreamedResponse` 같은 HTTP 응답을 직접 반환한다.

## 9) 비동기 구조 연결

비동기 처리는 API 계층과 분리되어 동작한다.

1. API/Action에서 Job dispatch
2. Redis Queue 적재
3. Horizon 워커 처리

운영 상세는 아래 문서 참고:

- Queue: `./queue.md`
- Scheduler: `./scheduler.md`

## 10) 현재 API 범위 요약

- Staff
  - 인증, 프로필/비밀번호 수정, 관리자 메모, 대시보드
  - 병원/입점신청/뷰티/일반회원/의료진/뷰티전문가 관리
  - 입점신청 목록/summary/상세/승인상태 변경
  - 병원 특징, 카테고리, 해시태그 관리
  - 병원 이벤트, 이벤트 DB, 리얼모델 DB 관리
  - 동영상 목록/상세/생성/수정/삭제/원본 다운로드
  - 토크/토크댓글, 병의원 후기/후기댓글, 병의원 평가/영수증 인증 관리
  - 신고게시물 관리, 신고 상태 처리, 경고/무시 처리, 채팅 메시지 신고 조회
  - 공지사항/FAQ CRUD와 에디터 이미지 업로드/정리
- Hospital
  - 인증, 프로필/비밀번호 수정, 관리자 메모
  - 병원 동영상 요청 생성과 파트너 취소
  - 현재 Hospital Actor API에는 동영상 목록/상세/수정 라우트가 없다.
- Beauty
  - 인증, 프로필/비밀번호 수정, 관리자 메모
  - 현재 Beauty Actor API에는 뷰티 파트너 업무 라우트가 없다.
- User
  - 인증, 프로필/비밀번호 수정
  - 채팅방/메시지/읽음/알림 설정, 채팅 메시지 신고
  - 토크 작성/삭제/댓글/투표/신고
  - 병의원 후기 작성/삭제/댓글/신고, 병의원 평가 신고
  - 병원 이벤트 DB/리얼모델 DB 신청
  - 사용자 차단/해제, 알림함/디바이스/알림 설정

## 11) 체크리스트

- [ ] 새 API가 Actor 경계에 맞게 배치됐는가?
- [ ] 컨트롤러가 얇게 유지되고 비즈니스 로직이 Domain으로 내려갔는가?
- [ ] 정책/권한/시더가 함께 갱신됐는가?
- [ ] 목록 응답이 `PaginatedResponse` 또는 명시적인 cursor pagination 규칙을 따르는가?
- [ ] 신고 대상 추가 시 `ContentReportTargetRegistry`, User 신고 라우트, Staff 신고게시물 라우트가 같이 갱신됐는가?
- [ ] 비동기 작업이 lane 정책(`critical`, `mail`, `sms`, `chat`, `default` 등)에 맞게 라우팅됐는가?
- [ ] 목록/summary/selector Query가 불필요한 N+1, 중복 count, 중복 인덱스를 만들지 않는가?
- [ ] 새 인덱스가 필요하면 기존 create migration에 반영할지, 운영용 add migration이 필요한지 결정했는가?

작성 기준: 2026-06-29
