# 콘텐츠 신고 / 신고게시물 관리

작성 기준: 2026-07-28

이 문서는 사용자 신고와 Staff 신고게시물 관리 구조를 정의한다. 현재는 설계 문서이며, 실제 구현 시 Java/Spring 패키지 구조에 맞춰 세부 클래스를 확정한다.

## 1. 범위

구현 대상:

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

신고 대상별 별도 테이블을 만들지 않고 공통 신고 도메인으로 관리한다.

## 2. 핵심 모델

| 모델 | 테이블 | 역할 |
| --- | --- | --- |
| `ContentReport` | `content_reports` | 사용자 신고 요청 1건의 공통 로그 |
| `ContentReportItem` | `content_report_items` | 신고 1건에 포함된 실제 신고 대상 항목 |
| `ContentReportState` | `content_report_states` | 신고 대상별 현재 신고 상태와 집계 |

`ContentReportState`는 `target_type + target_id`별로 1건만 유지한다. Staff 신고게시물 목록과 상세 화면은 이 상태 테이블을 기준으로 조회한다.

중복 신고 제한은 `content_report_items`의 `reporter_user_id + target_type + target_id` unique 기준이다.

## 3. 신고 대상

| alias | 대상 | 화면/도메인 |
| --- | --- | --- |
| `talk` | `Talk` | 토크 게시글 |
| `talk_comment` | `TalkComment` | 토크 댓글 |
| `hospital_review` | `HospitalReview` | 성형후기/시술후기 |
| `hospital_review_comment` | `HospitalReviewComment` | 후기 댓글 |
| `hospital_evaluation` | `HospitalEvaluation` | 병의원 평가 |
| `chat_message` | `ChatMessage` | 사용자 채팅 메시지 |
| `hospital_video` | `HospitalVideo` | 병의원 동영상 |

새 신고 대상을 추가할 때는 target registry, User 신고 API, Staff 신고게시물 API, 권한, 목록/상세 변환, summary cache, 운영 히스토리 대상을 같이 갱신한다.

## 4. 신고 사유

공통 신고 사유:

| 저장값 | 표시명 |
| --- | --- |
| `ABUSE` | 비방/욕설 |
| `SPAM` | 게시물/댓글 도배 |
| `ILLEGAL_AD` | 불법광고/홍보 |
| `PRIVACY_COPYRIGHT` | 개인정보/저작권 침해 |
| `OTHER` | 기타 |

`OTHER`일 때는 `reason_text`가 필수다.

동영상 신고는 별도 사유를 둘 수 있다.

| 저장값 | 표시명 |
| --- | --- |
| `DELETED_VIDEO` | 삭제된 동영상 |
| `WRONG_INFO` | 잘못된 정보 |
| `INAPPROPRIATE` | 부적절한 내용 |
| `OTHER` | 기타 |

## 5. 신고 상태

| 저장값 | 표시명 | 의미 |
| --- | --- | --- |
| `NONE` | 정상 | 신고 상태 없음 |
| `REPORTED` | 신고접수 | 사용자 신고가 접수됨 |
| `AUTO_BLOCKED` | 자동차단 | 기준치 초과로 자동 노출중지 |
| `ADMIN_HIDDEN` | 노출중지 | Staff가 노출중지 처리 |
| `NORMAL_VISIBLE` | 정상노출 | Staff가 신고오류/정상으로 판단 |
| `REEXPOSED` | 재노출 | 반복 정상노출 처리 |

경고 상태:

| 저장값 | 표시명 |
| --- | --- |
| `NONE` | 미처리 |
| `WARNED` | 경고 |
| `IGNORED` | 무시 |

## 6. 사용자 신고 흐름

1. 신고 대상 alias와 id를 검증한다.
2. 신고 사유와 `reason_text`를 검증한다.
3. 이미 신고한 대상이면 요청을 거부한다.
4. `content_reports`에 신고 로그를 저장한다.
5. `content_report_items`에 실제 신고 항목을 저장한다.
6. `content_report_states`를 생성하거나 카운트를 갱신한다.
7. 자동차단 기준을 넘으면 `AUTO_BLOCKED`로 변경하고 원본 콘텐츠를 미노출 처리한다.
8. summary cache를 무효화한다.

채팅 메시지 묶음 신고는 한 번에 최대 5개 메시지를 신고할 수 있다. 포함된 메시지 중 하나라도 이미 신고한 메시지면 요청 전체를 거부한다.

## 7. Staff 처리 흐름

Staff 처리는 가능한 한 통합 처리 API를 사용한다.

| 처리 | 의미 |
| --- | --- |
| `process` | 조치유형과 경고여부를 한 번에 처리 |
| `status` | 신고 상태만 직접 변경 |
| `warning-status` | 경고/무시만 직접 변경 |

통합 처리 검증:

- `target_type`: 지원 alias 중 하나
- `target_id`: 필수
- `report_status`: `ADMIN_HIDDEN` 또는 `NORMAL_VISIBLE`
- `process_reason`: 선택, 단 `ADMIN_HIDDEN`이면 필수
- `warning_status`: 선택, 단 일반 콘텐츠의 `ADMIN_HIDDEN` 처리에서는 필수

`NORMAL_VISIBLE` 처리에는 `warning_status`를 보낼 수 없다. 동영상 신고에는 `warning_status`를 보낼 수 없다.

## 8. 원본 콘텐츠 반영

일반 콘텐츠:

- `ADMIN_HIDDEN` -> 원본 `status = INACTIVE`
- `NORMAL_VISIBLE` -> 원본 `status = ACTIVE`

동영상:

- `ADMIN_HIDDEN` -> 원본 `admin_status = FORCED_STOPPED`
- `NORMAL_VISIBLE` -> 원본 `admin_status = NORMAL`

채팅 메시지는 원본 메시지 상태를 직접 바꾸지 않고 신고 상태에만 반영한다.

## 9. 운영 히스토리

신고 상태 변경과 경고/무시 변경은 운영 히스토리에 기록한다.

대표 변경 key:

| field_key | field_label | 의미 |
| --- | --- | --- |
| `report_status` | 조치유형 | 일반 신고 조치유형 변경 |
| `warning_status` | 경고여부 | 작성자 경고/무시 변경 |
| `status` | 노출여부 | 일반 콘텐츠 원본 노출 상태 변경 |
| `admin_status` | 강제중지 | 동영상 원본 강제중지 상태 변경 |

자동차단은 `SYSTEM` actor로 기록하고 metadata에 source를 남긴다.

## 10. Staff 권한

Staff 신고게시물 관리는 일반 게시물 관리 권한과 분리한다.

| target alias | 조회 권한 | 수정 권한 |
| --- | --- | --- |
| `talk`, `talk_comment` | `platform.reported_talk.show` | `platform.reported_talk.update` |
| `hospital_review`, `hospital_review_comment` | `platform.reported_hospital_review.show` | `platform.reported_hospital_review.update` |
| `hospital_evaluation` | `platform.reported_hospital_evaluation.show` | `platform.reported_hospital_evaluation.update` |
| `chat_message` | `platform.reported_chat_message.show` | `platform.reported_chat_message.update` |
| `hospital_video` | `platform.reported_video.show` | `platform.reported_video.update` |

## 11. API 방향

User 신고:

```text
POST /api/v1/user/talks/{id}/reports
POST /api/v1/user/talk-comments/{id}/reports
POST /api/v1/user/hospital-reviews/{id}/reports
POST /api/v1/user/hospital-review-comments/{id}/reports
POST /api/v1/user/hospital-evaluations/{id}/reports
POST /api/v1/user/videos/{id}/reports
POST /api/v1/user/chats/{id}/messages/reports
```

Staff 신고게시물:

```text
GET   /api/v1/staff/reported-contents/{target_type}
GET   /api/v1/staff/reported-contents/{target_type}/{target_id}
GET   /api/v1/staff/reported-contents/{target_type}/{target_id}/reports
PATCH /api/v1/staff/reported-contents/process
PATCH /api/v1/staff/reported-contents/status
PATCH /api/v1/staff/reported-contents/warning-status
```

목록 응답은 `PaginatedResponse`의 `items + meta` 구조를 따른다.

## 12. 표시 기준

공통 신고게시물:

- `REPORTED`, `AUTO_BLOCKED`: 신고접수 계열 표시
- `ADMIN_HIDDEN`: 노출중지
- `NORMAL_VISIBLE`, `REEXPOSED`: 정상노출/재노출

동영상 신고게시물:

- `REPORTED`, `AUTO_BLOCKED`: 신고접수
- `ADMIN_HIDDEN`: 삭제처리
- `NORMAL_VISIBLE`, `REEXPOSED`: 신고오류

## 13. 금지 사항

- `VALID`, `INVALID`를 신규 신고 상태로 추가하지 않는다.
- 채팅 신고를 Chat 도메인 권한 정책으로 직접 분기하지 않는다.
- 동영상 신고에 경고/무시를 붙이지 않는다.
- 신고 처리 없이 원본 콘텐츠 `status`만 직접 바꾸는 흐름을 만들지 않는다.
- 신고 대상 추가 시 registry, 권한, route, list/detail 변환, cache 무효화를 일부만 추가하지 않는다.
