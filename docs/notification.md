# 알림 설계

작성 기준: 2026-07-28

이 문서는 사용자 인앱 알림, push 발송, 알림 수신 설정의 백엔드 설계 기준을 정의한다. 현재는 구현 전 설계 문서이며, 실제 구현 시 Java/Spring 패키지 구조에 맞춰 세부 클래스를 확정한다.

## 1. 범위

1차 범위:

- 사용자 알림 목록 조회
- 사용자 안읽음 알림 수 조회
- 알림 단건 읽음 처리
- 알림 전체 읽음 처리
- 푸시 디바이스 등록/폐기
- 이벤트별 `in_app`, `push`, `email` 수신 설정 저장
- 채팅 메시지 생성 시 상대 사용자 알림 생성
- push delivery 생성

후순위 범위:

- WebSocket/SSE 실시간 알림
- 댓글, 좋아요, 후기, 이벤트 등 추가 도메인 알림 연결
- 이메일 발송
- 앱 badge count 동기화

## 2. 권장 패키지 구조

```text
domain/notification/
  NotificationInbox
  NotificationDelivery
  NotificationDevice
  NotificationPreference
  NotificationChannel
  NotificationDeliveryStatus

application/notification/
  NotificationUserService
  NotificationCreateService
  command/
  query/
  result/

adapter/in/web/user/notification/
  controller/
  request/

infrastructure/messaging/
infrastructure/redis/
```

알림은 여러 도메인에서 호출하는 공통 기능이다. 개별 도메인은 알림 테이블을 직접 만지지 않고 application 계층의 알림 생성 서비스를 호출한다.

## 3. 테이블 기준

### `notification_inboxes`

사용자에게 표시되는 인앱 알림 원장이다.

주요 컬럼:

- `recipient_type`, `recipient_id`: 수신자
- `actor_type`, `actor_id`: 알림 발생 주체
- `event_type`: 알림 이벤트 타입
- `title`, `body`: 표시 문구
- `aggregation_key`: 같은 알림 묶음을 판단하는 논리 키
- `open_aggregation_key`: 읽지 않은 묶음 하나만 유지하기 위한 유니크 잠금 키
- `event_count`: 묶인 실제 이벤트 수
- `target_type`, `target_id`: 이동 대상
- `payload`: 클라이언트 부가 데이터
- `read_at`: 읽음 시각

### `notification_deliveries`

채널별 발송 상태 이력이다.

- `notification_inbox_id`
- `channel`
- `status`
- `provider`
- `attempt_count`
- `sent_at`
- `failed_at`
- `error_message`

### `notification_devices`

push 발송 대상 디바이스다.

- `user_id`
- `platform`
- `device_token`
- `provider`
- `app_version`
- `locale`
- `revoked_at`

### `notification_preferences`

사용자별 이벤트 수신 설정이다.

- `user_id`
- `event_type`
- `in_app`
- `push`
- `email`

## 4. 생성 흐름

1. 도메인 유스케이스가 트랜잭션 안에서 원본 업무를 처리한다.
2. 알림이 필요한 경우 application 계층의 알림 생성 서비스를 호출한다.
3. 수신자, 이벤트 타입, 대상, 채널, payload를 검증한다.
4. 사용자 수신 설정과 활성 디바이스를 확인한다.
5. 가능한 채널이 없으면 알림을 생성하지 않는다.
6. 인앱 알림이 필요하면 `notification_inboxes`에 저장하거나 기존 unread 묶음을 갱신한다.
7. push가 필요하면 `notification_deliveries`에 `PENDING` 상태로 저장한다.
8. 비동기 발송이 준비되면 queue에서 push provider를 호출한다.

## 5. 집계 규칙

- `aggregation_key`가 있고 `aggregate=true`이면 unread 묶음 대상이다.
- 같은 수신자와 같은 `open_aggregation_key`의 unread row가 있으면 새 row를 만들지 않고 `event_count`를 증가시킨다.
- 읽음 처리 시 `read_at`을 채우고 `open_aggregation_key`를 비운다.
- 이후 같은 이벤트가 다시 오면 새 unread 묶음으로 생성된다.
- 최종 중복 방지는 DB unique key가 기준이다.

권장 key 형식:

```text
recipient:{recipient_type}:{recipient_id}:event:{event_type}:target:{target_type}:{target_id}
```

## 6. 채널 규칙

- `IN_APP`: 사용자 알림 목록에 노출한다.
- `PUSH`: 모바일 push delivery를 생성한다.
- `EMAIL`: 현재는 설정 저장만 하고 발송은 후순위다.
- `WEB`: 현재는 사용하지 않는다.

알림 payload에는 비밀번호, access token, reset token, 원문 인증정보를 넣지 않는다.

## 7. 채팅 연동 기준

채팅 메시지 생성 시 상대 participant에게 `chat.message.created` 알림을 만든다.

payload 예시:

```json
{
  "recipient_type": "USER",
  "recipient_id": 10,
  "actor_type": "USER",
  "actor_id": 20,
  "event_type": "chat.message.created",
  "title": "새 메시지가 도착했습니다.",
  "body": "메시지 미리보기",
  "aggregation_key": "recipient:USER:10:event:chat.message.created:target:CHAT:5",
  "target_type": "CHAT",
  "target_id": 5,
  "payload": {
    "chat_id": 5,
    "message_id": 100,
    "sender_user_id": 20
  },
  "channels": ["IN_APP", "PUSH"]
}
```

## 8. Push 발송

Push 발송은 queue 기반 비동기 처리로 둔다.

권장 설정:

- `PUSH_ENABLED`
- `PUSH_QUEUE`
- `PUSH_HTTP_TIMEOUT`
- `PUSH_PROVIDER`
- `FCM_ENABLED`
- `FCM_PROJECT_ID`
- `FCM_SERVICE_ACCOUNT_JSON`
- `APNS_ENABLED`
- `APNS_ENVIRONMENT`
- `APNS_TEAM_ID`
- `APNS_KEY_ID`
- `APNS_BUNDLE_ID`

발송 결과:

- 하나 이상의 device 발송 성공: `SENT`
- 모든 device 발송 실패: `FAILED`
- provider별 결과가 섞임: `MIXED`
- 실패 상세는 `error_message`에 저장한다.

invalid token으로 판단되면 device의 `revoked_at`을 채운다.

## 9. 신규 이벤트 추가 규칙

1. 이벤트 타입 문자열을 먼저 정한다.
2. 설정 화면에 노출할 이벤트면 기본 preference 목록에 추가한다.
3. 수신자와 actor 기준을 명확히 정한다.
4. 이동 대상이 있으면 `target_type`, `target_id`, `payload`를 일관되게 넣는다.
5. 반복 알림이면 안정적인 `aggregation_key`를 만든다.
6. push가 필요하면 `PUSH` 채널을 포함한다.
7. 알림 생성은 원본 업무 트랜잭션과의 실패 경계를 명확히 정한다.

## 10. 금지 사항

- 개별 도메인 Service가 알림 테이블을 직접 수정하지 않는다.
- push provider 실패로 원본 업무 트랜잭션을 롤백하지 않는다.
- 사용자별로 달라지는 값을 broadcast 공통 payload에 넣지 않는다.
- 민감정보를 알림 payload에 넣지 않는다.
