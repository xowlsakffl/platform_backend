# Queue 운영 가이드

작성 기준: 2026-07-27

이 문서는 Beaulab 프로젝트의 Queue, Redis, Horizon 구조와 운영 규칙을 정리한다.

스케줄러(`crontab`, Laravel Scheduler, Schedule Monitor)는 [scheduler.md](./scheduler.md)에서 관리한다.

## 1) 런타임 기준

Queue 표준 런타임은 Redis + Horizon이다.

현재 코드 기준:

- 기본 큐 연결: `config/queue.php`의 `default`, `.env`의 `QUEUE_CONNECTION=redis`
- Horizon 설정: `config/horizon.php`
- Horizon 접근: `/horizon`
- Horizon middleware: `web`, `internal_tool.ip`, `auth:tool_staff`
- Horizon Redis connection: `default`
- Horizon data prefix: `HORIZON_PREFIX` 또는 `APP_NAME` 기반 prefix

운영 환경에서는 `queue:listen`이나 개별 `queue:work`를 상시 실행하지 않는다. 큐 소비자는 Horizon 단일 체계로 운영한다.

## 2) 현재 실제 큐 작업

현재 코드에서 실제 큐에 올라가는 작업은 아래다.

| 작업 | 클래스/위치 | 연결 | 큐 | 용도 |
|---|---|---|---|---|
| 비밀번호 재설정 메일 | `PasswordResetLinkMail` | `PASSWORD_RESET_MAIL_QUEUE_CONNECTION` 기본 `redis` | `PASSWORD_RESET_MAIL_QUEUE` 기본 `mail` | 재설정 링크 메일 발송 |
| Push 발송 | `SendPushNotificationDeliveryJob` | `redis` | `PUSH_QUEUE` 기본 `notifications` | FCM/APNs 외부 발송 |

비밀번호 재설정 메일은 `Mail::queue()`로 발행한다. 메일 본문과 큐 설정은 `PasswordResetLinkSendAction`, `PasswordResetLinkMail`, `config/password_reset.php`를 기준으로 한다.

Push 발송은 `CreateNotificationAction`에서 `PUSH` delivery가 pending이면 `SendPushNotificationDeliveryJob`을 발행한다. 실제 외부 provider 호출은 `SendPushNotificationDeliveryAction`이 처리한다.

## 3) 큐 레인 정책

Horizon에는 현재와 향후 확장을 고려해 아래 레인을 표준으로 둔다.

| Queue | 현재 사용 | 용도 | 대기 경고 |
|---|---:|---|---:|
| `critical` | 예약 | 사용자 영향도가 큰 고우선 작업 | 10초 |
| `mail` | 사용 | 메일 발송 | 30초 |
| `sms` | 예약 | 문자 발송 | 30초 |
| `chat` | 예약 | 채팅 비동기 처리 | 15초 |
| `notifications` | 사용 | Push/알림 외부 발송 | 15초 |
| `default` | 예약 | 일반 비동기 작업 | 60초 |
| `maintenance` | 예약 | 정리, 백필, 유지보수 작업 | 180초 |

레인 추가/삭제/용도 변경 시 `config/horizon.php`, `.env.example`, 이 문서를 같이 갱신한다.

## 4) Horizon Supervisor 매핑

`config/horizon.php` 기준 supervisor:

| Supervisor | Queue | 기본 tries | 기본 timeout |
|---|---|---:|---:|
| `supervisor-critical` | `critical` | 3 | 30초 |
| `supervisor-mail` | `mail` | 3 | 90초 |
| `supervisor-sms` | `sms` | 5 | 60초 |
| `supervisor-chat` | `chat` | 2 | 30초 |
| `supervisor-notifications` | `notifications` | 2 | 30초 |
| `supervisor-default` | `default` | 3 | 90초 |
| `supervisor-maintenance` | `maintenance` | 1 | 300초 |

환경별 `maxProcesses`는 `config/horizon.php`의 `environments.production`, `environments.local`에서 override한다.

## 5) Queue 테이블 역할

| 테이블 | 역할 |
|---|---|
| `jobs` | `database` 큐 드라이버용 대기열. 현재 표준 운영은 Redis이므로 주 사용 대상 아님 |
| `job_batches` | `Bus::batch()` 메타데이터. 큐 백엔드가 Redis여도 DB 테이블 사용 |
| `failed_jobs` | 실패한 큐 작업 저장. 재시도/원인 분석/감사 용도 |

Horizon의 실시간 상태, 대기열, 처리량, supervisor 메타데이터는 Redis에 저장된다.

## 6) Job 작성 규칙

Job은 도메인 소유권 기준으로 둔다.

- 특정 도메인 전용: `app/Domains/{Domain}/Jobs`
- 공통 기능: `app/Domains/Common/{Feature}/Jobs` 또는 기존 공통 도메인 하위
- 기술 종류 기준 `app/Jobs` 몰아넣기는 지양한다.

작성 규칙:

1. `ShouldQueue`를 명시한다.
2. 생성자 또는 dispatch 시점에 `onConnection()`, `onQueue()`를 지정한다.
3. `$tries`, `$timeout`을 Job 성격에 맞게 명시한다.
4. 외부 API 호출 Job은 재시도해도 데이터가 깨지지 않게 멱등성을 보장한다.
5. DB 상태 변경 후 발행해야 하는 Job은 트랜잭션 commit 타이밍을 고려한다.
6. 레인 목적을 섞지 않는다. 예를 들어 Push 발송은 `notifications`, 메일은 `mail`을 사용한다.

주의:

`Illuminate\Bus\Queueable`을 쓰는 Job에서 `$connection`, `$queue`, `$delay` 속성을 직접 다시 선언하지 않는다. 충돌을 피하려면 생성자에서 아래처럼 지정한다.

```php
$this->onConnection('redis');
$this->onQueue('notifications');
```

## 7) 운영 명령어

Horizon:

```bash
php artisan horizon:status
php artisan horizon
php artisan horizon:terminate
```

실패 작업:

```bash
php artisan queue:failed
php artisan queue:retry all
php artisan queue:prune-failed --hours=168
```

Batch 메타 정리:

```bash
php artisan queue:prune-batches --hours=72 --unfinished=72 --cancelled=168
```

누락된 Push delivery 재큐잉:

```bash
php artisan notifications:send-pending-push --limit=100
```

## 8) 배포 체크리스트

1. 코드 배포
2. `php artisan migrate --force`
3. `php artisan config:cache` 적용 여부 확인
4. `php artisan horizon:terminate`
5. supervisor/systemd가 Horizon을 다시 띄우는지 확인
6. `php artisan horizon:status` 확인
7. Horizon 대시보드에서 레인별 대기열과 실패 Job 확인

`horizon:terminate`는 워커가 새 코드와 새 config를 읽게 하는 목적이다. 운영에서 Horizon을 직접 foreground로 띄우는 명령이 아니다.

## 9) 장애 대응

### 큐 적체

1. Horizon 대시보드에서 적체 레인 확인
2. 특정 레인만 적체면 해당 Job의 외부 API/DB 병목 확인
3. 일시 증가면 production `maxProcesses` 상향 검토
4. 반복 적체면 Job 분리, batch 크기 조정, 쿼리 최적화 검토

### 실패 Job 급증

1. `php artisan queue:failed`로 실패 클래스/예외 확인
2. 외부 API 장애인지, 코드 오류인지 분리
3. 코드 수정 후 재시도 가능한 Job만 `queue:retry` 실행
4. 영구 실패 데이터는 원인 기록 후 정리

### Push 발송 누락

1. `notification_deliveries`에서 `channel=PUSH`, `status=PENDING` 확인
2. Horizon의 `notifications` 큐 상태 확인
3. 필요 시 `php artisan notifications:send-pending-push --limit=100` 실행

### 메일 발송 누락

1. `mail` 큐 적체 확인
2. 로컬이면 Mailpit 실행 여부 확인
3. 운영이면 SMTP/SES/Resend 등 외부 provider 설정 확인
4. 실패 Job에 남은 `PasswordResetLinkMail` 재시도 여부 판단

## 10) 향후 적용 우선순위

Queue로 옮기기 좋은 작업:

- 이미지 리사이즈/썸네일 생성/압축
- 엑셀 다운로드 생성
- 광고 승인 후 알림/캐시 무효화 후처리
- 신고 처리 후 알림/summary 캐시 무효화 후처리
- 통계/집계 재계산

단, 결제/포인트/광고 구좌 확정처럼 정합성이 중요한 작업은 DB transaction과 unique key를 기준으로 처리하고, Queue는 알림/후처리/비동기 보조 작업에 사용한다.
