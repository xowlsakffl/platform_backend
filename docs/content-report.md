# 콘텐츠 신고 / 신고게시물 관리

- 작성 기준: 2026-07-24
- 기준 코드: `App\Domains\Common\ContentReport`, `app/Modules/User/Http/Requests/ContentReport`, `app/Modules/Staff/Http/Requests/ContentReport`, `ContentReportTargetRegistry`

이 문서는 현재 코드 기준의 콘텐츠 신고 기능과 Staff 신고게시물 관리 구조를 정리한다.

## 1. 현재 범위

구현 대상은 다음으로 제한한다.

- 토크 게시글 신고
- 토크 댓글 신고
- 성형후기/시술후기 신고
- 후기 댓글 신고
- 병의원 평가 신고
- 채팅 메시지 묶음 신고
- 병의원 동영상 신고
- 신고게시물 목록/상세/신고내역 조회
- 관리자 조치유형 처리
- 관리자 경고/무시 처리
- 신고게시물 summary cache

현재는 신고 대상별 별도 테이블을 만들지 않고 공통 신고 도메인으로 관리한다.

## 2. 핵심 모델

| 모델 | 테이블 | 역할 |
|---|---|---|
| `ContentReport` | `content_reports` | 사용자 신고 요청 1건의 공통 로그 |
| `ContentReportItem` | `content_report_items` | 신고 1건에 포함된 실제 신고 대상 항목 |
| `ContentReportState` | `content_report_states` | 신고 대상별 현재 신고 상태와 집계 |

`ContentReport`는 신고자, 대표 신고 대상, 신고 사유, 신고 당시 스냅샷을 저장한다.

`ContentReportItem`은 실제 신고된 항목을 저장한다. 일반 게시글/댓글/평가/동영상 신고는 item 1건이고, 채팅 메시지 신고는 한 번에 최대 5개 item을 저장한다.

`ContentReportState`는 `target_type + target_id`별로 1건만 유지한다. Staff 신고게시물 목록과 상세 화면은 이 상태 테이블을 기준으로 조회한다.

중복 신고 제한은 `content_report_items`의 `reporter_user_id + target_type + target_id` unique 기준이다. 채팅 메시지 묶음 신고에서 대표 메시지가 달라도, 이미 신고한 메시지가 하나라도 포함되면 요청 전체를 거부한다.

## 3. 신고 대상

신고 대상 alias는 `ContentReportTargetRegistry`에서 관리한다.

| alias | 모델 | 화면/도메인 |
|---|---|---|
| `talk` | `Talk` | 토크 게시글 |
| `talk_comment` | `TalkComment` | 토크 댓글 |
| `hospital_review` | `HospitalReview` | 성형후기/시술후기 |
| `hospital_review_comment` | `HospitalReviewComment` | 후기 댓글 |
| `hospital_evaluation` | `HospitalEvaluation` | 병의원 평가 |
| `chat_message` | `ChatMessage` | 사용자 채팅 메시지 |
| `hospital_video` | `HospitalVideo` | 병의원 동영상 |

새 신고 대상을 추가할 때는 다음을 같이 갱신한다.

- `ContentReportTargetRegistry`
- User 신고 라우트와 Request
- Staff 신고게시물 라우트
- `ContentReportStateForStaffPolicy` 권한 매핑
- 목록/상세 DTO 변환
- summary cache 무효화 대상
- 운영 히스토리 대상 registry

지원하지 않는 target alias는 `ContentReportTargetRegistry::classForAlias()` 단계에서 거부한다. 신규 신고 대상은 권한 매핑을 추가하기 전까지 Staff 조회/수정이 불가능해야 한다.

## 4. 신고 사유

### 4.1 공통 신고 사유

공통 신고 Request는 `ContentReport::reasons()`를 기준으로 검증한다.

| 저장값 | 표시명 |
|---|---|
| `ABUSE` | 비방/욕설 |
| `SPAM` | 게시물/댓글 도배 |
| `ILLEGAL_AD` | 불법광고/홍보 |
| `PRIVACY_COPYRIGHT` | 개인정보/저작권 침해 |
| `OTHER` | 기타 |

`OTHER`일 때는 `reason_text`가 필수다.

### 4.2 동영상 신고 사유

병의원 동영상 신고 Request는 `ContentReport::hospitalVideoReasons()`를 기준으로 별도 검증한다.

| 저장값 | 표시명 |
|---|---|
| `DELETED_VIDEO` | 삭제된 동영상 |
| `ABUSE` | 비방/욕설 |
| `ILLEGAL_AD` | 불법광고/홍보 |

동영상 신고에는 `OTHER`, `reason_text`, `SPAM`, `PRIVACY_COPYRIGHT`를 사용하지 않는다.

## 5. 신고 상태

`ContentReportState.report_status`는 다음 상태만 사용한다.

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `NONE` | 없음 | 신고 접수 전 기본값 |
| `REPORTED` | 신고접수 | 신고가 접수되어 관리자 처리가 필요한 상태 |
| `AUTO_BLOCKED` | 자동차단 | 1시간 내 신고 10건 이상으로 자동 미노출 처리된 상태 |
| `ADMIN_HIDDEN` | 노출중지 | 관리자가 신고를 인정하고 미노출 처리한 상태 |
| `NORMAL_VISIBLE` | 정상노출 | 관리자가 신고를 인정하지 않고 정상노출 처리한 상태 |
| `REEXPOSED` | 재노출 | 정상노출 처리 3회차부터 자동 전이 잠금이 걸린 상태 |

`VALID`, `INVALID`는 현재 백엔드 신고 상태가 아니다. 신규 API, DB seed, 프론트 요청 payload, 문서에는 사용하지 않는다.

게시글/댓글/후기/평가 대상은 `AUTO_BLOCKED`, `ADMIN_HIDDEN` 상태일 때 일반 게시물관리 화면에서 노출/미노출 변경을 잠근다. 해당 상태는 신고게시물 관리에서만 정상노출/노출중지로 처리한다.

채팅 메시지는 원본 콘텐츠에 `status` 컬럼이 없으므로 `ContentReportState`만 변경한다. 신고 상태를 바꿔도 채팅 메시지 원본 row의 노출 상태를 직접 수정하지 않는다.

병의원 동영상은 원본 상태 컬럼이 `status`가 아니라 `admin_status`다. 동영상 신고에서 `ADMIN_HIDDEN`은 동영상 `admin_status = FORCED_STOPPED`, `NORMAL_VISIBLE`은 `admin_status = NORMAL`로 반영한다.

## 6. 경고 상태

`ContentReportState.warning_status`는 다음 상태만 사용한다.

| 저장값 | 표시명 | 의미 |
|---|---|---|
| `NONE` | 미처리 | 경고/무시 미처리 |
| `WARNED` | 경고 | 신고 대상 작성자에게 경고 반영 |
| `IGNORED` | 무시 | 신고 대상은 처리했지만 작성자 경고는 하지 않음 |

경고/무시는 작성자 계정이 있는 신고 대상에만 적용한다. 현재 동영상 신고에는 경고/무시를 적용하지 않는다.

`WARNED`로 바꾸면 대상 작성자의 `AccountUser.warning_count`가 1 증가한다. 누적 경고가 `AccountUser::WARNING_BLOCK_THRESHOLD` 이상이면 사용자는 `BLOCKED` 상태가 된다.

`WARNED`에서 `IGNORED`로 변경하면 경고 수가 1 감소하고, 경고 수가 차단 기준 미만이 되면 차단 상태를 해제한다.

## 7. 사용자 신고 생성 흐름

신고 생성은 `ContentReportCreateForUserAction`이 담당한다.

1. `ContentReportTargetRegistry::assertSupported()`로 신고 대상 지원 여부를 확인한다.
2. `ContentReportItem` 기준 중복 신고 여부를 확인한다.
3. `content_reports`에 신고 로그를 생성한다.
4. `content_report_items`에 실제 신고 항목을 생성한다.
5. `content_report_states`를 `target_type + target_id` 기준으로 잠금 조회하거나 생성한다.
6. `report_count`, `recent_hour_report_count`, `first_reported_at`, `last_reported_at`을 갱신한다.
7. 동영상 72시간 재신고 제한 정책을 먼저 적용한다.
8. 자동 처리 잠금 여부를 확인한다.
9. 최근 1시간 신고 수가 기준 이상이면 자동차단을 적용한다.
10. 자동차단 기준 미만이고 기존 상태가 `NONE` 또는 `NORMAL_VISIBLE`이면 `REPORTED`로 바꾼다.
11. 대상별 신고 summary cache를 무효화한다.

`recent_hour_report_count`는 단순 최근 1시간이 아니라 `normal_visible_at` 이후라면 그 시각을 기준으로 다시 계산한다. 정상노출 직후 이전 신고가 누적되어 바로 자동차단되는 상황을 막기 위한 기준이다.

## 8. 자동 상태 전이

자동차단 기준은 `ContentReportState::AUTO_BLOCK_RECENT_HOUR_THRESHOLD`다. 현재 값은 최근 1시간 10건이다.

자동 전이 기준:

- 대상 모델에 `status` 컬럼이 있어야 한다.
- 최근 1시간 신고 수가 10건 이상이어야 한다.
- 현재 상태가 `ADMIN_HIDDEN`이면 자동 전이하지 않는다.
- 정상노출 처리 횟수가 3회 이상이면 자동 신고접수/자동차단 전이에서 제외한다.

자동차단 시 대상 콘텐츠는 `status = INACTIVE`로 바꾸고, 시스템 actor로 운영 히스토리를 남긴다.

채팅 메시지는 `status` 컬럼이 없으므로 자동차단으로 원본 row를 바꾸지 않는다. 신고 집계와 Staff 처리 상태만 유지한다.

## 9. 동영상 신고 특수 정책

병의원 동영상 신고는 신고 상태는 공통 `ContentReportState`를 쓰지만, 운영 표시와 대상 상태 변경은 동영상 정책을 따른다.

| 내부 상태 | 동영상 신고 화면 표시 | 동영상 대상 상태 |
|---|---|---|
| `REPORTED` | 신고접수 | 변경 없음 |
| `AUTO_BLOCKED` | 신고접수 | 변경 없음 |
| `ADMIN_HIDDEN` | 삭제처리 | `admin_status = FORCED_STOPPED` |
| `NORMAL_VISIBLE` | 신고오류 | `admin_status = NORMAL` |
| `REEXPOSED` | 신고오류 | `admin_status = NORMAL` |

동영상 신고오류 72시간 정책:

- `NORMAL_VISIBLE` 또는 `REEXPOSED` 처리 후 72시간 이내 추가 신고가 들어오면 현재 상태를 유지하고 신고 로그/카운트만 누적한다.
- 72시간이 지난 뒤 추가 신고가 들어오면 자동차단 판단보다 먼저 `REPORTED`로 전환한다.
- 이 정책은 병의원 동영상에만 적용한다.

동영상 신고 처리 모달 문구는 다음 정책을 따른다.

- 삭제처리: `해당 영상을 삭제처리(미노출) 하시겠습니까?`
- 신고오류: `해당 신고 건을 오류(허위신고)로 처리하시겠습니까?`
- 신고오류 보조 문구: `확인 시 72시간 동안 동일 영상에 대한 신규 신고 접수가 제한됩니다.`

## 10. 채팅 메시지 신고 정책

채팅 메시지 신고는 `/api/v1/user/chats/{chat}/messages/reports`에서 처리한다.

- 한 번에 1~5개 메시지를 신고할 수 있다.
- 신고자는 해당 채팅방 참여자여야 한다.
- 본인이 작성한 메시지는 신고할 수 없다.
- 한 번의 요청에는 같은 사용자가 작성한 메시지만 포함할 수 있다.
- 이미 차단한 사용자의 메시지는 신고할 수 없다.
- 신고 성공 시 신고자는 해당 메시지 작성자를 자동 차단한다.
- 차단과 동시에 기존 대화는 신고자 기준으로 숨김 처리한다.
- 같은 신고자가 같은 메시지를 다시 신고할 수 없다.
- 같은 메시지가 하나라도 포함된 묶음 신고는 전체 요청을 거부한다.

채팅 메시지 신고의 `ContentReport.target_id`는 요청 메시지 중 첫 번째 메시지를 대표 대상으로 사용한다. 실제 신고된 메시지들은 `ContentReportItem`으로 저장한다.

채팅 메시지는 원본 `ChatMessage`에 `status`가 없으므로 Staff 조치유형은 `ContentReportState`에만 반영한다. 작성자 제재가 필요하면 `warning_status`로 처리한다.

## 11. Staff 처리 흐름

Staff 처리는 세 가지 엔드포인트가 있다.

| Action | 용도 |
|---|---|
| `ContentReportProcessForStaffAction` | 조치유형과 경고여부를 한 번에 처리하는 권장 흐름 |
| `ContentReportStateStatusUpdateForStaffAction` | 신고 상태만 직접 변경 |
| `ContentReportWarningStatusUpdateForStaffAction` | 경고/무시만 직접 변경 |

신규 화면은 가능한 `/reported-contents/process`를 사용한다. 조치유형과 경고여부가 분리되면 상태와 작성자 제재가 어긋나기 쉽기 때문이다.

### 11.1 `/process` 검증

`ContentReportProcessForStaffRequest` 기준:

- `target_type`: `ContentReportTargetRegistry::aliases()` 중 하나
- `target_id`: 필수 정수
- `report_status`: `ADMIN_HIDDEN` 또는 `NORMAL_VISIBLE`
- `process_reason`: 선택 문자열, 단 `ADMIN_HIDDEN`이면 필수
- `warning_status`: 선택 문자열, 단 일반 콘텐츠의 `ADMIN_HIDDEN` 처리에서는 필수

검증 정책:

- `NORMAL_VISIBLE` 처리에는 `warning_status`를 보낼 수 없다.
- `ADMIN_HIDDEN` 처리에는 `process_reason`이 필요하다.
- 동영상 신고에는 `warning_status`를 보낼 수 없다.
- 동영상이 아닌 신고 대상의 `ADMIN_HIDDEN` 처리에는 `warning_status`가 필요하다.

### 11.2 조치유형 처리

| 요청 `report_status` | 결과 |
|---|---|
| `ADMIN_HIDDEN` | 신고 상태를 노출중지로 바꾸고 대상 콘텐츠를 미노출 계열로 변경 |
| `NORMAL_VISIBLE` | 신고 상태를 정상노출로 바꾸고 대상 콘텐츠를 노출 계열로 변경 |

일반 콘텐츠는 대상 테이블에 `status` 컬럼이 있으면 다음처럼 반영한다.

- `ADMIN_HIDDEN` -> `status = INACTIVE`
- `NORMAL_VISIBLE` -> `status = ACTIVE`

동영상은 다음처럼 반영한다.

- `ADMIN_HIDDEN` -> `admin_status = FORCED_STOPPED`
- `NORMAL_VISIBLE` -> `admin_status = NORMAL`

`NORMAL_VISIBLE` 처리 시 기존 상태가 `NORMAL_VISIBLE`/`REEXPOSED`가 아니면 `normal_visible_count`를 1 증가시킨다. 처리 횟수가 3회 이상이면 저장 상태는 `REEXPOSED`가 된다. 이때 `recent_hour_report_count`는 0으로 초기화한다.

## 12. 운영 히스토리

신고 상태 변경과 경고/무시 변경은 `OperationHistory::ACTION_STATE_UPDATED`로 기록한다.

변경 상세는 `operation_history_changes`에 저장된다. `OperationHistoryCreateAction`은 `field_label` 저장 시 `변경` 접미사를 제거한다. 따라서 호출부는 `조치유형 변경`, `경고여부 변경`, `신고상태 변경`, `노출여부 변경`, `강제중지 변경`처럼 넘겨도 저장 label은 화면 공통 formatter 기준으로 정규화된다.

대표 변경 key:

| field_key | field_label 호출 기준 | 의미 |
|---|---|---|
| `report_status` | `조치유형 변경` | 일반 신고 조치유형 변경 |
| `report_status` | `신고상태 변경` | 동영상 신고 상태 변경 |
| `warning_status` | `경고여부 변경` | 작성자 경고/무시 변경 |
| `status` | `노출여부 변경` | 일반 콘텐츠 원본 노출 상태 변경 |
| `admin_status` | `강제중지 변경` | 동영상 원본 강제중지 상태 변경 |

자동차단은 system actor로 기록하고, metadata에 `source = user.content_report.auto_block`을 남긴다.

Staff 통합 처리(`/process`)는 metadata에 `source = staff.content_report.process`를 남긴다.

## 13. Staff 권한

Staff 신고게시물 관리는 일반 게시물 관리 권한과 분리한다. `ContentReportStatePolicy`는 Staff actor일 때 `ContentReportStateForStaffPolicy`로 위임하고, target alias별 권한을 매핑한다.

| target alias | 조회 권한 | 수정 권한 |
|---|---|---|
| `talk`, `talk_comment` | `BEAULAB_REPORTED_TALK_SHOW` | `BEAULAB_REPORTED_TALK_UPDATE` |
| `hospital_review`, `hospital_review_comment` | `BEAULAB_REPORTED_HOSPITAL_REVIEW_SHOW` | `BEAULAB_REPORTED_HOSPITAL_REVIEW_UPDATE` |
| `hospital_evaluation` | `BEAULAB_REPORTED_HOSPITAL_EVALUATION_SHOW` | `BEAULAB_REPORTED_HOSPITAL_EVALUATION_UPDATE` |
| `chat_message` | `BEAULAB_REPORTED_CHAT_MESSAGE_SHOW` | `BEAULAB_REPORTED_CHAT_MESSAGE_UPDATE` |
| `hospital_video` | `BEAULAB_REPORTED_VIDEO_SHOW` | `BEAULAB_REPORTED_VIDEO_UPDATE` |

권한 매핑이 없는 alias는 조회/수정 모두 false다.

## 14. API 경로

### 14.1 User 신고 생성

| Method | Path | Request |
|---|---|---|
| `POST` | `/api/v1/user/talks/{talk}/reports` | `ContentReportCreateForUserRequest` |
| `POST` | `/api/v1/user/talks/{talk}/comments/{comment}/reports` | `ContentReportCreateForUserRequest` |
| `POST` | `/api/v1/user/hospital-reviews/{hospitalReview}/reports` | `ContentReportCreateForUserRequest` |
| `POST` | `/api/v1/user/hospital-reviews/{hospitalReview}/comments/{comment}/reports` | `ContentReportCreateForUserRequest` |
| `POST` | `/api/v1/user/hospital-evaluations/{hospitalEvaluation}/reports` | `ContentReportCreateForUserRequest` |
| `POST` | `/api/v1/user/videos/{video}/reports` | `HospitalVideoReportForUserRequest` |
| `POST` | `/api/v1/user/chats/{chat}/messages/reports` | `ChatMessageReportForUserRequest` |

### 14.2 Staff 신고게시물 관리

| Method | Path |
|---|---|
| `GET` | `/api/v1/staff/reported-contents/talks/summary` |
| `GET` | `/api/v1/staff/reported-contents/talks` |
| `GET` | `/api/v1/staff/reported-contents/talk-comments/summary` |
| `GET` | `/api/v1/staff/reported-contents/talk-comments` |
| `GET` | `/api/v1/staff/reported-contents/hospital-reviews/surgery/summary` |
| `GET` | `/api/v1/staff/reported-contents/hospital-reviews/surgery` |
| `GET` | `/api/v1/staff/reported-contents/hospital-reviews/treatment/summary` |
| `GET` | `/api/v1/staff/reported-contents/hospital-reviews/treatment` |
| `GET` | `/api/v1/staff/reported-contents/hospital-review-comments/surgery/summary` |
| `GET` | `/api/v1/staff/reported-contents/hospital-review-comments/surgery` |
| `GET` | `/api/v1/staff/reported-contents/hospital-review-comments/treatment/summary` |
| `GET` | `/api/v1/staff/reported-contents/hospital-review-comments/treatment` |
| `GET` | `/api/v1/staff/reported-contents/hospital-evaluations/summary` |
| `GET` | `/api/v1/staff/reported-contents/hospital-evaluations` |
| `GET` | `/api/v1/staff/reported-contents/chats/summary` |
| `GET` | `/api/v1/staff/reported-contents/chats` |
| `GET` | `/api/v1/staff/reported-contents/videos/summary` |
| `GET` | `/api/v1/staff/reported-contents/videos` |
| `GET` | `/api/v1/staff/reported-contents/detail/{targetType}/{targetId}` |
| `GET` | `/api/v1/staff/reported-contents/{targetType}/{targetId}/reports` |
| `PATCH` | `/api/v1/staff/reported-contents/process` |
| `PATCH` | `/api/v1/staff/reported-contents/status` |
| `PATCH` | `/api/v1/staff/reported-contents/warning-status` |

목록 응답은 `PaginatedResponse`의 `items + meta` 구조다. summary/detail/process/status/warning-status는 단일 payload를 `ApiResponse::success()`로 반환한다.

신고내역은 `reports_page` 파라미터를 사용하고, 페이지당 10건이다.

## 15. 목록 필터와 summary

목록 필터는 `ReportedContentListForStaffRequest` 기준이다.

| 파라미터 | 의미 |
|---|---|
| `q` | 검색어 |
| `search_type` | `all`, `id`, `nickname`, `hospital_name`, `content` |
| `target_author_id` | 작성자 ID |
| `date_type` | `created_at`, `first_reported_at`, `last_message_at` |
| `report_reason` | 신고 사유. 공통 사유와 동영상 사유 합집합 허용 |
| `report_count_min`, `report_count_max` | 신고 수 범위 |
| `target_status` | 원본 콘텐츠 상태. 일반 콘텐츠는 `ACTIVE`/`INACTIVE`, 동영상은 `NORMAL`/`FORCED_STOPPED` |
| `warning_status` | 경고 처리 상태 |
| `summary_filter` | summary 카드 클릭 필터 |
| `report_status` | 신고 처리 상태 배열 |
| `category_domain` | 후기/후기 댓글의 성형/쁘띠 구분 |
| `start_date`, `end_date` | 기간 |
| `sort`, `direction`, `per_page` | 정렬/페이지 크기 |

summary 키:

| key | 기준 |
|---|---|
| `reported_or_auto_blocked_count` | `report_status in (REPORTED, AUTO_BLOCKED)` |
| `today_report_count` | 오늘 생성된 `content_reports` 수 |
| `recent_30_days_admin_hidden_count` | 최근 30일 내 `ADMIN_HIDDEN` 처리된 대상 수 |
| `recent_30_days_normal_visible_count` | 최근 30일 내 `NORMAL_VISIBLE` 또는 `REEXPOSED` 처리된 대상 수 |

`target_author_id` 필터가 있는 summary는 캐시하지 않는다. 특정 회원 상세에서 보는 summary는 사용자별 조건이 들어가므로 공통 캐시에 섞으면 안 된다.

## 16. 목록 성능 기준

신고게시물 목록은 `content_report_states`를 기준으로 조회한다.

- 대상 콘텐츠는 `loadMorph()`로 target별 필요한 관계만 로드한다.
- 현재 페이지의 신고 요약은 target 묶음 기준으로 한 번에 조회한다.
- 최신 신고는 `MAX(id)`를 target별로 구한 뒤 한 번에 로드한다.
- 신고 사유별 건수는 `target_type + target_id + reason` 그룹으로 조회한다.
- 신고내역 모달은 `/reported-contents/{targetType}/{targetId}/reports`로 별도 페이지네이션한다.

상단 summary는 `ContentReportSummaryCache`를 통해 `StaffSummaryCache::DOMAIN_CONTENT_REPORT`에 캐싱한다. cache key는 `targetClass + categoryDomain/all + today` 조합이다.

cache 무효화 기준:

- 사용자 신고 생성 후 `ContentReportSummaryCache::forgetForTarget()`
- 관리자 조치유형 처리 후 `ContentReportSummaryCache::forgetForTarget()`
- 관리자 경고/무시 처리 후 `ContentReportSummaryCache::forgetForTarget()`
- 동영상 신고 대상이면 `StaffSummaryCache::DOMAIN_HOSPITAL_VIDEO`도 같이 무효화
- 경고/무시가 회원 상태/경고 수를 바꿀 수 있으므로 `StaffSummaryCache::DOMAIN_ACCOUNT_USER`도 무효화

## 17. 프론트 표시 원칙

프론트는 백엔드 상태값을 임의로 새 상태로 바꾸지 않는다. 화면별 문구 변환은 표시 계층에서만 처리한다.

공통 신고게시물:

- `REPORTED`, `AUTO_BLOCKED` -> 신고접수 계열 표시
- `ADMIN_HIDDEN` -> 노출중지
- `NORMAL_VISIBLE`, `REEXPOSED` -> 정상노출/재노출

동영상 신고게시물:

- `REPORTED`, `AUTO_BLOCKED` -> 신고접수
- `ADMIN_HIDDEN` -> 삭제처리
- `NORMAL_VISIBLE`, `REEXPOSED` -> 신고오류

댓글 신고 목록에는 상세 페이지가 없다. 신고 사유/신고횟수를 클릭하면 신고내역 모달을 띄우고, 조치유형과 경고여부는 모달 플로우에서 처리한다.

## 18. 금지 사항

- `VALID`, `INVALID`를 신규 신고 상태로 추가하지 않는다.
- 채팅 신고를 Chat 도메인 Policy로 직접 분기하지 않는다. Staff 신고게시물 권한은 `ContentReportStateForStaffPolicy`에서 target alias 기준으로 판단한다.
- 동영상 신고에 경고/무시를 붙이지 않는다.
- 동영상 신고오류 72시간 제한을 다른 신고 대상에 적용하지 않는다.
- 신고 처리 없이 원본 콘텐츠 `status`만 직접 바꾸는 흐름을 만들지 않는다.
- 신고 대상 추가 시 registry, 권한, route, list/detail 변환, cache 무효화를 일부만 추가하지 않는다.
