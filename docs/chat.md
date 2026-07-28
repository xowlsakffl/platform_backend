# 채팅 설계

- 작성 기준: 2026-07-24
- 기준 코드: `App\Domains\Chat`, `app/Modules/User/Http/Requests/Chat`, `routes/channels.php`, `ContentReportTargetRegistry`
- 현재 스코프: 앱 사용자(`account_users`) 간 1:1 채팅

## 1. 현재 범위

구현 대상은 다음으로 제한한다.

- 앱 사용자 간 1:1 채팅
- 텍스트/이미지/파일 메시지 발송
- 채팅방별 알림 on/off
- 읽음 상태 저장 및 실시간 전달
- 사용자별 채팅 삭제
- 사용자 차단
- 채팅 메시지 신고
- 신고된 채팅 메시지의 직원 관리자 처리

현재는 그룹 채팅, 병원계정-직원관리자 채팅, 운영자 상담 채팅, 별도 채팅 마이크로서비스, Kafka, NoSQL 채팅 저장소로 가지 않는다.

## 2. 기술 선택

| 구분 | 현재 기준 |
|---|---|
| 백엔드 | Laravel |
| 영속 저장 | MySQL |
| 실시간 전달 | Laravel Reverb private channel |
| 비동기 처리 | Redis Queue + Horizon |
| 푸시 알림 | 공통 Notification 도메인 |
| 파일 첨부 | 공통 `Media` 폴리모픽 |
| 신고 처리 | 공통 `ContentReport` 도메인 |

메시지 저장과 브로드캐스트는 분리한다. 메시지는 DB 트랜잭션으로 먼저 저장하고, Reverb 이벤트는 `ShouldBroadcastNow`로 즉시 발행한다. Redis Queue는 채팅 메시지 저장 자체가 아니라 푸시 알림 발송 같은 후속 비동기 작업에 사용한다.

## 3. 핵심 원칙

### 3.1 메시지는 DB 저장이 우선이다

실시간 전달은 저장 성공 이후의 후속 동작이다.

1. 채팅방 조회 또는 생성
2. 메시지 저장
3. 채팅방 `last_message_id`, `last_message_at` 갱신
4. 발신자 읽음 커서 갱신
5. 트랜잭션 종료
6. Reverb 메시지 이벤트 발행
7. 수신자 알림 생성 및 푸시 발송 Job 연결

Reverb 브로드캐스트가 실패해도 저장된 메시지가 기준이다. 클라이언트는 목록/메시지 조회 API로 최종 상태를 복구할 수 있어야 한다.

### 3.2 1:1 채팅방은 사용자쌍당 1개다

중복 채팅방을 막기 위해 `chats.match_key`를 사용한다.

- 형식: `{작은 user id}:{큰 user id}`
- 예시: `12:48`
- 생성기: `ChatMatchKey::forUsers()`
- DB 제약: `chats.match_key` unique

동시에 첫 메시지가 들어오면 unique 충돌이 날 수 있으므로, 중복키 예외를 잡고 기존 방을 다시 잠금 조회한다.

### 3.3 채팅방은 첫 메시지에서 생성한다

채팅 UI 진입만으로 서버 채팅방을 만들지 않는다.

- 상대 선택/채팅 UI 진입: 클라이언트 로컬 작성 상태
- 첫 메시지 전송: `chats`, `chat_participants`, `chat_messages`를 트랜잭션으로 생성
- 메시지를 보내지 않고 이탈: 서버 데이터 없음
- 채팅 목록 조회: `last_message_id`가 있는 채팅방만 노출

## 4. 테이블 구조

### 4.1 `chats`

채팅방 헤더 테이블이다.

- `status`: `ACTIVE`, `SUSPENDED`, `CLOSED`
- `match_key`: 정렬된 두 사용자 ID 기반 1:1 매칭 키
- `created_by_user_id`: 최초 생성 사용자
- `last_message_id`, `last_message_at`: 목록 조회용 역정규화 값
- `metadata`: 채팅방 확장 메타데이터
- `closed_at`: 종료 시각
- `deleted_at`: 채팅방 소프트 삭제

관련 파일: `database/migrations/2026_04_10_110000_create_chats_table.php`

### 4.2 `chat_participants`

채팅방 참여자별 상태 테이블이다.

- `chat_id`
- `account_user_id`
- `last_read_message_id`, `last_read_at`
- `deleted_until_message_id`: 사용자별 숨김 커서
- `deleted_at`: 사용자별 채팅 삭제 시각
- `notifications_enabled`: 채팅방별 알림 수신 여부

참여자는 현재 서비스 로직상 정확히 2명이다. DB는 `chat_id + account_user_id` 중복만 막고, “2명만 참여” 규칙은 서비스 로직에서 강제한다.

관련 파일: `database/migrations/2026_04_10_110100_create_chat_participants_table.php`

### 4.3 `chat_messages`

실제 메시지 저장 테이블이다.

- `chat_id`
- `sender_user_id`
- `client_message_id`: 클라이언트 재전송 멱등 키
- `message_type`: `TEXT`, `IMAGE`, `FILE`
- `body`
- `reply_to_message_id`
- `metadata`
- `edited_at`
- `deleted_at`

`client_message_id`는 `chat_id + sender_user_id + client_message_id` unique다. 단, MySQL unique에서 `NULL`은 중복 허용이므로 멱등성이 필요한 클라이언트는 반드시 `client_message_id`를 보내야 한다.

관련 파일: `database/migrations/2026_04_10_110200_create_chat_messages_table.php`

### 4.4 `account_user_blocks`

앱 사용자 간 방향성 차단 테이블이다.

- `blocker_user_id`: 차단한 사용자
- `blocked_user_id`: 차단된 사용자
- `blocked_at`: 차단 시각

차단은 방향성이 있다. A가 B를 차단해도 B가 A를 차단한 row가 자동 생성되지는 않는다. 다만 메시지 발송은 어느 한쪽이라도 차단 관계가 있으면 막는다.

관련 파일: `database/migrations/2026_04_10_110600_create_account_user_blocks_table.php`

## 5. 메시지 발송

첫 메시지와 기존 채팅방 메시지는 같은 Action(`ChatMessageSendForUserAction`)을 사용한다.

### 5.1 첫 메시지

요청:

- `peer_user_id`: 필수
- `message_type`: 기본 `TEXT`
- `body`: `TEXT`면 필수
- `attachments`: `IMAGE`, `FILE`이면 필수
- `client_message_id`: 선택
- `reply_to_message_id`: 선택
- `metadata`: 선택

처리:

1. 본인에게 보내는 요청 차단
2. 상대 사용자가 존재하고 활성 상태인지 확인
3. `match_key` 생성
4. 채팅방 잠금 조회 또는 생성
5. participant 2건 생성 또는 보정
6. 차단 관계 확인
7. 메시지 저장
8. 첨부 저장
9. 브로드캐스트와 알림 처리

### 5.2 기존 채팅방 메시지

기존 채팅방 전송은 route model binding으로 받은 `Chat`을 잠금 조회한 뒤 처리한다.

검증:

- 채팅방 상태가 `ACTIVE`여야 한다.
- 발신자가 participant여야 한다.
- 상대와 양방향 차단 관계가 없어야 한다.
- `reply_to_message_id`가 있으면 같은 채팅방의 메시지여야 한다.

### 5.3 첨부파일

첨부는 `ChatMessage`의 `attachments` collection으로 공통 `Media`에 저장한다.

- `IMAGE`: `image` validation 적용
- `FILE`: 일반 파일 허용
- 최대 개수: 10개
- 파일당 최대 용량: 50MB
- 메시지 생성 재시도에서 이미 첨부가 있으면 중복 첨부하지 않는다.

## 6. 조회/읽음/삭제

### 6.1 채팅 목록

API: `GET /api/v1/user/chats`

파라미터:

- `per_page`: 1~50, 기본 20
- `include_closed`: 종료 채팅 포함 여부

조회 기준:

- `last_message_id`가 있는 채팅방만 노출한다.
- 현재 사용자가 participant인 방만 노출한다.
- 사용자별 삭제 커서가 있으면 그 이후 새 메시지가 생긴 방만 다시 노출한다.
- 기본은 `ACTIVE`만 조회한다.
- 정렬은 `last_message_at desc`, `id desc`다.
- `unread_count`는 현재 사용자의 읽음 커서와 삭제 커서를 모두 지난 상대 메시지만 계산한다.

### 6.2 메시지 목록

API: `GET /api/v1/user/chats/{chat}/messages`

파라미터:

- `per_page`: 1~100, 기본 30
- `after_id`: 해당 ID 이후 메시지 정방향 조회
- `before_id`: 해당 ID 이전 메시지 역방향 조회

`after_id`와 `before_id`는 동시에 보낼 수 없다. 기본 조회와 `before_id` 조회는 최신 메시지부터 역순으로 가져오고, `after_id` 조회는 새 메시지를 붙이기 위해 오름차순으로 가져온다.

사용자별 삭제 커서(`deleted_until_message_id`) 이전 메시지는 다시 내려주지 않는다.

### 6.3 읽음 처리

API: `POST /api/v1/user/chats/{chat}/read`

- `last_read_message_id`가 없으면 현재 채팅방 마지막 메시지까지 읽음 처리한다.
- 값이 있으면 같은 채팅방 메시지인지 확인한다.
- 읽음 커서는 앞으로만 이동한다.
- 처리 후 `.chat.read.updated` 이벤트를 발행한다.

### 6.4 채팅 삭제

API: `DELETE /api/v1/user/chats/{chat}`

사용자별 삭제다. 채팅방, 메시지, 상대방 participant를 삭제하지 않는다.

처리:

- 현재 `last_message_id`를 `deleted_until_message_id`로 저장
- `deleted_at` 저장
- 삭제 시점까지 읽은 것으로 `last_read_message_id`, `last_read_at` 갱신
- 이후 새 메시지가 생기면 같은 채팅방이 다시 목록에 나타난다.

## 7. 차단

차단은 채팅방 상태가 아니라 사용자 관계 상태다.

API:

- `GET /api/v1/user/blocks`
- `POST /api/v1/user/blocks`
- `DELETE /api/v1/user/blocks/{blockedUserId}`

차단 시:

- `account_user_blocks`에 방향성 row 생성
- 기존 1:1 채팅방이 있으면 차단한 사용자에게만 채팅방을 숨긴다.
- 상대방 채팅방과 메시지는 유지한다.
- 이후 양쪽 어느 방향이든 메시지 발송은 저장 전에 차단한다.

에러 메시지:

- 내가 상대를 차단한 경우: `차단 해제 후 메시지를 보낼 수 있습니다.`
- 상대가 나를 차단한 경우: `메시지를 보낼 수 없습니다.`

## 8. 채팅 신고

API: `POST /api/v1/user/chats/{chat}/messages/reports`

파라미터:

- `message_ids`: 필수, 1~5개
- `reason`: 필수, `ContentReport::reasons()`
- `reason_text`: `OTHER`일 때 필수

처리 기준:

- 신고자는 해당 채팅방 participant여야 한다.
- 사용자별 삭제 커서 이전 메시지는 신고할 수 없다.
- 본인이 보낸 메시지는 신고할 수 없다.
- 한 번에 신고하는 메시지는 같은 채팅방, 같은 발신자의 메시지여야 한다.
- 이미 신고한 메시지는 다시 신고할 수 없다.
- 신고 생성 시 대표 메시지는 첫 번째 메시지다.
- 신고 대상은 `chat_message` alias로 공통 `ContentReport`에 저장한다.
- 여러 메시지를 신고하면 `content_report_items`에 각 메시지를 저장한다.
- 신고와 동시에 신고 대상 사용자를 차단하고, 신고자에게만 기존 채팅방을 숨긴다.

채팅 메시지는 `status` 컬럼이 없다. 따라서 신고 처리로 원본 `chat_messages`의 노출 상태를 직접 바꾸지 않는다. 신고 상태, 조치유형, 경고여부는 `ContentReportState`와 운영 히스토리로 관리한다.

## 9. 직원 관리자 신고 처리

채팅 신고는 공통 신고게시물 구조를 사용한다.

- target alias: `chat_message`
- 권한:
  - 조회: `BEAULAB_REPORTED_CHAT_MESSAGE_SHOW`
  - 수정: `BEAULAB_REPORTED_CHAT_MESSAGE_UPDATE`
- 목록/상세: `ContentReport` 직원 API
- 상세 payload에는 신고 메시지, 첨부, 신고자/작성자 정보, 신고 사유, 운영 히스토리가 포함된다.

처리 상태는 `ContentReportState` 기준이다.

| 상태 | 운영 표시 |
|---|---|
| `REPORTED` | 신고접수 |
| `AUTO_BLOCKED` | 자동차단 |
| `ADMIN_HIDDEN` | 노출중지 |
| `NORMAL_VISIBLE` | 정상노출 |
| `REEXPOSED` | 재노출 |

직원 처리 API에서 직접 선택 가능한 조치유형은 `ADMIN_HIDDEN`, `NORMAL_VISIBLE`이다. `ADMIN_HIDDEN` 처리 시 채팅 메시지에는 status 컬럼이 없으므로 원문 메시지를 숨기지는 않고, 신고 상태와 경고여부만 기록한다.

경고여부:

- `WARNED`: 신고 대상 작성자(`sender_user_id`)의 `warning_count` 증가
- `IGNORED`: 경고 무시 처리
- 경고 누적이 `AccountUser::WARNING_BLOCK_THRESHOLD` 이상이면 일반회원 상태가 차단으로 전환될 수 있다.

## 10. 실시간 채널

브로드캐스트 인증은 `bootstrap/app.php`의 `withBroadcasting()`을 사용한다.

- 미들웨어: `api`, `auth:sanctum`, `abilities:actor:user`
- 채널: `private-chat.{chatId}`
- 채널 권한: 해당 채팅방 participant만 구독 가능

이벤트:

| 이벤트 | broadcast name | payload |
|---|---|---|
| 메시지 생성 | `.chat.message.created` | `message` |
| 읽음 변경 | `.chat.read.updated` | `chat_id`, `reader_user_id`, `last_read_message_id`, `last_read_at` |

브로드캐스트 메시지 payload에는 `is_mine`을 넣지 않는다. 모든 구독자에게 같은 payload가 가야 하므로, 앱은 `sender_user_id`와 현재 로그인 사용자 ID를 비교해서 내 메시지 여부를 판단한다. 사용자 API 응답 DTO에는 현재 사용자 기준 `is_mine`을 포함한다.

## 11. 알림

메시지 저장 후 상대 participant 중 `notifications_enabled = true`인 사용자에게 공통 알림을 생성한다.

- event type: `chat.message.created`
- target type: `CHAT`
- target id: `chat_id`
- channels: `IN_APP`, `PUSH`
- aggregation key: 수신자 + 이벤트 + 채팅방 기준

인앱 알림 이벤트는 즉시 발행하고, 푸시 발송은 `SendPushNotificationDeliveryJob`으로 큐에 위임한다. 실제 푸시 발송 가능 여부는 공통 Notification preference/device 설정을 따른다.

## 12. 금지 기준

- 채팅 UI 진입만으로 빈 채팅방을 생성하지 않는다.
- 같은 사용자쌍의 채팅방을 여러 개 만들지 않는다.
- 메시지를 저장하기 전에 Reverb/Push를 먼저 보내지 않는다.
- `client_message_id` 없이 재시도 멱등성을 기대하지 않는다.
- 사용자별 채팅 삭제를 물리 삭제로 처리하지 않는다.
- 차단 사실을 상대에게 직접 노출하는 에러 문구를 사용하지 않는다.
- 신고된 채팅 메시지를 게시물처럼 `status` 컬럼 변경으로 숨긴다고 가정하지 않는다.
- 브로드캐스트 DTO에 `is_mine` 같은 사용자별 필드를 넣지 않는다.

## 13. 향후 확장 포인트

병원계정 ↔ 직원관리자 채팅이 실제 구현 대상이 되면 별도 설계가 필요하다.

- `support_chats` 또는 별도 상담 채팅 도메인
- 담당자 배정
- 운영 메모 연결
- 상태 전이 세분화
- SLA/응답 지연 관리
- 웹 관리자 실시간 UI 정책
