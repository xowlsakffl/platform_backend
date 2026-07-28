# 채팅 설계

작성 기준: 2026-07-28

이 문서는 일반 사용자 간 채팅, 차단, 채팅 메시지 신고의 백엔드 설계 기준을 정의한다. 현재는 구현 전 설계 문서이며, 실제 구현 시 Java/Spring 패키지 구조에 맞춰 세부 클래스를 확정한다.

## 1. 범위

- 사용자 간 1:1 채팅방
- 채팅 메시지 목록
- 메시지 전송
- 채팅방 숨김
- 사용자 차단
- 채팅 메시지 묶음 신고
- 알림 연동

## 2. 핵심 테이블

### `chats`

채팅방 원장이다.

주요 컬럼:

- `match_key`: 참여자 조합으로 만든 고유 키
- `last_message_id`
- `last_message_at`
- `created_at`, `updated_at`

`match_key`는 사용자 2명의 id를 정렬해 안정적으로 만든다.

### `chat_participants`

채팅방 참여자다.

주요 컬럼:

- `chat_id`
- `user_id`
- `last_read_message_id`
- `hidden_at`
- `blocked_at`

### `chat_messages`

채팅 메시지다.

주요 컬럼:

- `chat_id`
- `sender_user_id`
- `body`
- `sent_at`
- `deleted_at`

### `account_user_blocks`

사용자 차단 관계다.

주요 컬럼:

- `blocker_user_id`
- `blocked_user_id`
- `reason`
- `created_at`

## 3. 권장 패키지 구조

```text
domain/chat/
  Chat
  ChatParticipant
  ChatMessage

application/chat/
  ChatUserService
  command/
  query/
  result/

adapter/in/web/user/chat/
  controller/
  request/

infrastructure/persistence/chat/
```

신고 상태는 채팅 도메인이 직접 소유하지 않고 공통 콘텐츠 신고 도메인과 연동한다.

## 4. 채팅방 생성 기준

- 사용자 2명의 id를 정렬해 `match_key`를 만든다.
- 같은 `match_key` 채팅방이 있으면 기존 방을 재사용한다.
- 차단 관계가 있으면 새 메시지 전송을 막는다.
- 숨김 처리된 참여자가 새 메시지를 보내거나 받으면 필요에 따라 숨김을 해제한다.

## 5. 메시지 전송 흐름

1. 발신자와 수신자를 확인한다.
2. 차단 관계를 확인한다.
3. 기존 채팅방을 찾거나 생성한다.
4. 메시지를 저장한다.
5. 채팅방의 마지막 메시지 정보를 갱신한다.
6. 참여자의 숨김 상태를 필요한 경우 갱신한다.
7. 상대 사용자에게 `chat.message.created` 알림을 생성한다.

알림 payload에는 `chat_id`, `message_id`, `sender_user_id` 정도만 넣는다. 사용자별로 달라지는 `is_mine` 같은 값은 알림 payload에 넣지 않는다.

## 6. 읽음 처리

- 사용자가 채팅방을 열면 `last_read_message_id`를 갱신한다.
- 읽음 처리는 메시지별 row를 만들지 않고 participant 상태로 관리한다.
- 읽지 않은 수는 `last_read_message_id` 이후 메시지 수로 계산한다.

## 7. 숨김과 차단

숨김:

- 특정 사용자 기준으로 채팅방 목록에서 숨긴다.
- 상대방에게는 영향을 주지 않는다.
- 새 메시지가 오면 정책에 따라 다시 목록에 노출할 수 있다.

차단:

- 차단한 사용자는 상대에게 메시지를 보낼 수 없다.
- 차단된 사용자도 차단자에게 메시지를 보낼 수 없다.
- 차단과 동시에 기존 대화는 차단자 기준으로 숨김 처리할 수 있다.

## 8. 채팅 메시지 신고

채팅 메시지 신고는 공통 콘텐츠 신고 도메인을 사용한다.

규칙:

- 한 번에 최대 5개 메시지를 신고할 수 있다.
- 같은 신고자가 같은 메시지를 다시 신고할 수 없다.
- 포함된 메시지 중 하나라도 이미 신고했다면 요청 전체를 거부한다.
- `ContentReport.target_id`는 요청 메시지 중 첫 번째 메시지를 대표 대상으로 사용한다.
- 실제 신고된 메시지는 `ContentReportItem`으로 저장한다.
- 채팅 메시지는 원본 `status`를 직접 바꾸지 않고 `ContentReportState`에만 조치 상태를 저장한다.

권한:

- 조회: `platform.reported_chat_message.show`
- 수정: `platform.reported_chat_message.update`

## 9. API 방향

User API:

```text
GET    /api/v1/user/chats
GET    /api/v1/user/chats/{id}/messages
POST   /api/v1/user/chats
POST   /api/v1/user/chats/{id}/messages
PATCH  /api/v1/user/chats/{id}/read
DELETE /api/v1/user/chats/{id}
POST   /api/v1/user/chats/{id}/messages/reports
POST   /api/v1/user/blocks
DELETE /api/v1/user/blocks/{id}
```

Staff 신고게시물 API는 [콘텐츠 신고 문서](./content-report.md)를 따른다.

## 10. 응답 원칙

- 메시지 응답에는 현재 사용자 기준 `is_mine`을 포함할 수 있다.
- broadcast 공통 payload에는 `is_mine`을 넣지 않는다.
- 작성자 정보는 필요한 최소 필드만 내려준다.
- 신고 상태와 메시지 본문은 권한에 따라 마스킹할 수 있다.

## 11. 금지 사항

- 차단 관계를 프론트에서만 막지 않는다.
- 채팅 메시지 신고를 채팅 전용 신고 테이블로 분리하지 않는다.
- 메시지 원문을 알림 payload나 로그에 과도하게 남기지 않는다.
- broadcast payload에 사용자별 계산값을 넣지 않는다.
