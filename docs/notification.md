# Notification 운영 가이드

작성 기준: 2026-07-27

이 문서는 현재 코드 기준의 공통 알림 도메인을 정리한다. 현재 구현 범위는 앱 사용자 `USER` 대상 인앱 알림, 실시간 Reverb broadcast, 모바일/웹 Push delivery, 사용자별 알림 수신 설정이다.

기준 파일:

- `app/Domains/Common/Notification`
- `app/Modules/User/Http/Controllers/Notification/NotificationForUserController.php`
- `app/Modules/User/Http/Requests/Notification`
- `app/Modules/User/routes/api_user.php`
- `routes/channels.php`
- `config/notification_push.php`
- `database/migrations/2026_04_10_110500_create_notification_tables.php`

## 1) 현재 구현 범위

구현됨:

- 앱 사용자 알림 목록 조회
- 앱 사용자 안읽음 알림 수 조회
- 알림 단건 읽음 처리
- 알림 전체 읽음 처리
- 푸시 디바이스 등록/폐기
- 이벤트별 `in_app`, `push`, `email` 수신 설정 저장
- 채팅 메시지 생성 시 상대 사용자 알림 생성
- 인앱 알림 Reverb broadcast
- Push delivery 생성 및 Redis queue 기반 FCM/APNs 발송

현재 제한:

- 수신자 타입은 현재 `USER`만 broadcast 대상으로 동작한다.
- 기본 알림 이벤트는 `chat.message.created`만 정의되어 있다.
- 댓글, 좋아요, 후기 등 다른 도메인 알림은 아직 연결되어 있지 않다.
- `EMAIL`, `WEB` 채널 상수와 DB 컬럼은 있으나 현재 생성 플로우에서는 지원 채널로 받지 않는다.
- 알림 설정의 `email` 값은 저장만 가능하고 발송에는 아직 사용되지 않는다.

## 2) 코드 위치

도메인 구조:

- Action: `app/Domains/Common/Notification/Actions`
- User Action: `app/Domains/Common/Notification/Actions/User`
- Query: `app/Domains/Common/Notification/Queries`
- User Query: `app/Domains/Common/Notification/Queries/User`
- Model: `app/Domains/Common/Notification/Models`
- DTO: `app/Domains/Common/Notification/Dto`
- Event: `app/Domains/Common/Notification/Events`
- Job: `app/Domains/Common/Notification/Jobs`

모듈 구조:

- Controller: `app/Modules/User/Http/Controllers/Notification/NotificationForUserController.php`
- Request: `app/Modules/User/Http/Requests/Notification`
- Route: `app/Modules/User/routes/api_user.php`

알림은 여러 도메인에서 호출할 수 있는 공통 기능이므로 `Common\Notification` 아래에 둔다. 채팅 같은 개별 도메인은 `CreateNotificationAction`만 호출하고, 저장/집계/발송 세부 규칙은 알림 도메인이 담당한다.

## 3) 테이블

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
- `target_type`, `target_id`: 이동 대상 엔티티
- `payload`: 클라이언트 부가 데이터
- `read_at`: 읽음 시각

인덱스:

- `recipient_type`, `recipient_id`, `read_at`, `created_at`
- `event_type`, `created_at`
- `target_type`, `target_id`
- `recipient_type`, `recipient_id`, `open_aggregation_key` unique
- `recipient_type`, `recipient_id`, `aggregation_key`, `read_at`

### `notification_deliveries`

채널별 발송 상태 이력이다.

주요 컬럼:

- `notification_inbox_id`
- `channel`: `IN_APP`, `PUSH`, `EMAIL`, `WEB`
- `status`: `PENDING`, `SENT`, `FAILED`
- `provider`: `REVERB`, `FCM`, `APNS`, `MIXED`
- `provider_message_id`
- `attempted_at`, `delivered_at`, `failed_at`
- `error_message`

현재 동작:

- `IN_APP` delivery는 생성 즉시 `SENT`, provider `REVERB`로 저장한다.
- `PUSH` delivery는 `PENDING`으로 저장한 뒤 `SendPushNotificationDeliveryJob`이 처리한다.
- `EMAIL`, `WEB` delivery는 아직 실제 발송 구현이 없다.

### `notification_devices`

Push 발송 대상 디바이스 토큰이다.

주요 컬럼:

- `owner_type`, `owner_id`
- `platform`: `IOS`, `ANDROID`, `WEB`
- `device_uuid`
- `push_token`
- `push_token_hash`
- `app_version`
- `last_seen_at`
- `revoked_at`
- `metadata`

규칙:

- 토큰 원문은 긴 Web Push endpoint를 고려해 `text`로 저장한다.
- 중복 판단은 `push_token_hash = sha256(push_token)` 기준이다.
- 같은 사용자, 같은 platform, 같은 `device_uuid`에서 새 토큰이 등록되면 기존 활성 토큰은 `revoked_at` 처리한다.
- revoke는 push token 원문을 받아 hash로 찾아 비활성 처리한다.

### `notification_preferences`

사용자별 이벤트 수신 설정이다.

주요 컬럼:

- `owner_type`, `owner_id`
- `event_type`
- `in_app`
- `push`
- `email`
- `metadata`

기본값:

- `in_app = true`
- `push = true`
- `email = false`

현재 기본 이벤트 타입:

- `chat.message.created`

주의:

- 현재 Request는 `event_type` 문자열만 검증하고 whitelist는 강제하지 않는다.
- 신규 이벤트를 공식 지원하려면 `NotificationPreference::DEFAULT_EVENT_TYPES`에 추가해 설정 목록에 기본 노출되게 한다.

## 4) 생성 플로우

공통 진입점:

- `CreateNotificationAction::execute(array $payload)`

처리 순서:

1. payload를 정규화한다.
2. 요청 채널을 현재 지원 채널인 `IN_APP`, `PUSH` 중 유효한 값으로 정리한다.
3. 사용자 preference와 active device를 확인해 실제 가능한 채널만 남긴다.
4. 가능한 채널이 없으면 알림을 만들지 않고 `null`을 반환한다.
5. `open_aggregation_key`를 채널 범위로 스코프 처리한다.
6. `NotificationCreateQuery`가 unread 집계 row를 갱신하거나 새 row를 생성한다.
7. `IN_APP` 채널이 있으면 `NotificationInboxUpdated` 이벤트를 dispatch한다.
8. `PUSH` delivery가 `PENDING`이면 `SendPushNotificationDeliveryJob`을 dispatch한다.

채널 필터:

- `IN_APP`: preference `in_app`이 꺼져 있으면 생성하지 않는다.
- `PUSH`: preference `push`가 켜져 있고 활성 device가 있을 때만 생성한다.
- `EMAIL`, `WEB`: 현재 미지원 채널이므로 `CreateNotificationAction`에서 무시한다.

## 5) 집계 규칙

집계 목적:

- 같은 사용자의 같은 대상 알림이 반복될 때 알림 row를 무한정 늘리지 않는다.
- 목록에서는 unread 묶음 1건과 `event_count`를 사용해 "외 N건" 표현이 가능하게 한다.

규칙:

- `aggregation_key`가 있고 `aggregate = true`이면 unread 묶음 대상이다.
- 읽지 않은 같은 `open_aggregation_key` row가 있으면 새 row를 만들지 않고 기존 row의 `event_count`를 증가시킨다.
- 읽음 처리 시 `read_at`을 채우고 `open_aggregation_key`를 `null`로 바꾼다.
- 이후 같은 이벤트가 다시 오면 새 unread 묶음으로 생성된다.
- 읽음 처리는 알림 정렬용 `updated_at`을 흔들지 않도록 timestamps를 끈 상태로 저장한다.

채널 스코프:

- `CreateNotificationAction`은 `open_aggregation_key`를 그대로 쓰지 않고 아래 형태로 바꾼다.
- `channels:in_app:{sha1(aggregation_key)}`
- `channels:non_in_app:{sha1(aggregation_key)}`

이유:

- 인앱 알림이 꺼진 상태의 push-only 알림과 인앱 알림 row가 같은 unread bucket을 공유하면 목록 노출 기준이 꼬일 수 있다.
- 사용자 목록은 `IN_APP` delivery가 있는 알림만 조회한다.

동시성:

- `NotificationCreateQuery`는 `lockForUpdate()`로 기존 unread row를 잠근다.
- 유니크 키 충돌이 발생하면 1회 재시도한다.
- 최종 중복 방지는 DB unique key가 기준이다.

## 6) 채팅 연동

현재 실제 연결 지점:

- `app/Domains/Chat/Actions/User/ChatMessageSendForUserAction.php`

이벤트:

- `chat.message.created`

수신자:

- 현재 메시지 sender를 제외한 채팅 participant

payload 예:

```php
[
    'recipient_type' => NotificationInbox::RECIPIENT_USER,
    'recipient_id' => $recipientId,
    'actor_type' => NotificationInbox::ACTOR_USER,
    'actor_id' => $sender->id,
    'event_type' => NotificationInbox::EVENT_CHAT_MESSAGE_CREATED,
    'title' => $sender->nickname ?? '새 메시지가 도착했습니다.',
    'body' => $messageBody,
    'aggregation_key' => "recipient:user:{$recipientId}:event:chat.message.created:target:chat:{$chatId}",
    'target_type' => NotificationInbox::TARGET_CHAT,
    'target_id' => $chatId,
    'payload' => [
        'chat_id' => $chatId,
        'message_id' => $messageId,
        'sender_user_id' => $senderId,
        'sender_user_nickname' => $senderNickname,
    ],
    'channels' => ['IN_APP', 'PUSH'],
]
```

채팅 메시지 중복 처리:

- `client_message_id` 재시도로 기존 메시지를 반환하는 경우, 새 메시지 생성이 아니므로 알림을 중복 생성하지 않는다.
- 첨부파일 생성이 새로 발생한 경우에는 실시간 메시지 이벤트와 알림 생성을 진행한다.

## 7) User API

모든 알림 API는 아래 미들웨어 안에 있다.

- `auth:sanctum`
- `abilities:actor:user`
- `EnsureActiveUser`

엔드포인트:

- `GET /api/v1/user/notifications`
  - 내 알림 목록
  - 필터: `per_page`, `unread_only`, `event_type`, `target_type`, `target_id`
  - `per_page` 범위: 1-50, 기본 20
- `GET /api/v1/user/notifications/unread-count`
  - `unread_count`: unread 알림 묶음 수
  - `unread_event_count`: unread 묶음의 `event_count` 합계
- `POST /api/v1/user/notifications/read-all`
  - 내 unread 알림 전체 읽음
  - 응답: `read_count`
- `POST /api/v1/user/notifications/devices`
  - Push token 등록
  - 입력: `platform`, `push_token`, `device_uuid`, `app_version`, `metadata`
- `POST /api/v1/user/notifications/devices/revoke`
  - Push token 폐기
  - 입력: `push_token`
- `GET /api/v1/user/notifications/preferences`
  - 이벤트별 수신 설정 목록
  - 저장된 설정이 없어도 기본 이벤트 타입은 기본값으로 내려준다.
- `PUT|PATCH /api/v1/user/notifications/preferences`
  - 이벤트별 수신 설정 변경
  - 입력: `event_type`, `in_app`, `push`, `email`, `metadata`
  - `in_app`, `push`, `email`, `metadata` 중 하나 이상 필요
- `POST /api/v1/user/notifications/{notificationInbox}/read`
  - 단건 읽음
  - 본인 알림이 아니면 `FORBIDDEN`

## 8) 실시간 알림

Broadcast channel:

- `private-user.{userId}`
- 실제 채널명 정의: `routes/channels.php`의 `user.{userId}`
- 본인 `AccountUser` ID와 채널 userId가 같을 때만 구독 가능

Event:

- 클래스: `NotificationInboxUpdated`
- broadcast name: `notification.inbox.updated`
- interface: `ShouldBroadcastNow`
- payload: `{ notification: NotificationInboxDto }`

현재 제한:

- `broadcastWhen()`에서 `recipient_type === USER`인 알림만 broadcast한다.

## 9) Push 발송

설정 파일:

- `config/notification_push.php`

주요 env:

- `PUSH_ENABLED`
- `PUSH_QUEUE`
- `PUSH_HTTP_TIMEOUT`
- `PUSH_PROVIDER`
- `PUSH_IOS_PROVIDER`
- `PUSH_ANDROID_PROVIDER`
- `PUSH_WEB_PROVIDER`
- `FCM_ENABLED`
- `FCM_PROJECT_ID`
- `FCM_CLIENT_EMAIL`
- `FCM_PRIVATE_KEY`
- `FCM_SERVICE_ACCOUNT_PATH`
- `FCM_SERVICE_ACCOUNT_JSON`
- `FCM_TOKEN_URI`
- `FCM_SCOPE`
- `APNS_ENABLED`
- `APNS_ENVIRONMENT`
- `APNS_TEAM_ID`
- `APNS_KEY_ID`
- `APNS_BUNDLE_ID`
- `APNS_PRIVATE_KEY`
- `APNS_PRIVATE_KEY_PATH`

Queue:

- Job: `SendPushNotificationDeliveryJob`
- connection: `redis`
- queue: `notifications` 또는 `PUSH_QUEUE`
- tries: 3
- timeout: 45초

Provider 선택:

- `IOS`: `PUSH_IOS_PROVIDER`, 기본 `PUSH_PROVIDER`, 기본값 `fcm`
- `ANDROID`: `PUSH_ANDROID_PROVIDER`, 기본 `PUSH_PROVIDER`, 기본값 `fcm`
- `WEB`: `PUSH_WEB_PROVIDER`, 기본 `PUSH_PROVIDER`, 기본값 `fcm`

FCM:

- OAuth access token은 cache에 55분 저장한다.
- service account는 JSON 문자열, 파일 경로, 개별 env 값을 지원한다.
- 실패 응답은 delivery `error_message`와 앱 warning 로그에 남긴다.
- invalid token으로 판단되면 device `revoked_at`을 채운다.

APNs:

- provider token은 cache에 50분 저장한다.
- `BadDeviceToken`, `Unregistered`는 invalid token으로 보고 device를 revoke한다.

발송 결과:

- 하나 이상의 device 발송이 성공하면 delivery는 `SENT`
- 모든 device 발송이 실패하면 delivery는 `FAILED`
- provider가 여러 종류면 `MIXED`
- 실패 상세는 `error_message`에 2000자까지 저장한다.

## 10) 신규 알림 이벤트 추가 규칙

1. 이벤트 타입 문자열을 먼저 정한다.
   - 예: `talk.comment.created`
2. 가능하면 `NotificationInbox`에 상수로 추가한다.
3. 사용자가 설정 화면에서 기본으로 보게 할 이벤트라면 `NotificationPreference::DEFAULT_EVENT_TYPES`에 추가한다.
4. 생성 도메인의 Action에서 트랜잭션 완료 후 `CreateNotificationAction`을 호출한다.
5. `recipient_type`, `recipient_id`, `event_type`은 반드시 넣는다.
6. 목록 이동이 필요한 알림은 `target_type`, `target_id`, `payload`를 일관되게 넣는다.
7. 집계 알림이면 안정적인 `aggregation_key`를 만든다.
8. 집계하지 않을 알림이면 `aggregate => false`를 명시한다.
9. 푸시가 필요하면 `channels`에 `PUSH`를 포함한다.
10. 알림 payload에는 비밀번호, access token, reset token, 원문 인증정보를 넣지 않는다.

권장 aggregation key 형식:

```text
recipient:{type}:{id}:event:{event_type}:target:{target_type}:{target_id}
```

## 11) 운영 점검

알림이 안 보일 때:

- `notification_inboxes` row가 생성됐는지 확인한다.
- `notification_deliveries`에 `IN_APP` delivery가 있는지 확인한다.
- 사용자 preference의 `in_app`이 꺼져 있지 않은지 확인한다.
- `read_at`이 이미 채워져 있지 않은지 확인한다.

푸시가 안 갈 때:

- `PUSH_ENABLED=true`인지 확인한다.
- platform별 provider 설정을 확인한다.
- active device가 있는지 확인한다.
- 사용자 preference의 `push`가 꺼져 있지 않은지 확인한다.
- `notification_deliveries`의 `PUSH` row 상태와 `error_message`를 확인한다.
- Redis queue worker 또는 Horizon이 `notifications` queue를 처리 중인지 확인한다.
- FCM/APNs credential env를 확인한다.

실시간 broadcast가 안 될 때:

- `routes/channels.php`의 `user.{userId}` 인증이 통과하는지 확인한다.
- 클라이언트가 `private-user.{userId}` 채널을 구독하는지 확인한다.
- Reverb 설정과 broadcast connection을 확인한다.
- `NotificationInboxUpdated`가 `recipient_type = USER`로 dispatch되는지 확인한다.

## 12) 문서 기준으로 지켜야 할 것

- 알림 원장은 DB가 기준이다.
- 실시간 broadcast와 push는 원장 생성 이후의 delivery 수단이다.
- Push는 API 요청 안에서 직접 외부 호출하지 않고 queue job으로 처리한다.
- 사용자가 읽음 처리한 알림 row는 다시 unread 묶음으로 열지 않는다.
- 일반 도메인은 알림 저장 구조를 직접 만지지 말고 `CreateNotificationAction`을 호출한다.
- 신규 채널을 추가할 때는 `notification_deliveries`의 상태 전이와 실패 기록을 먼저 정의한다.
