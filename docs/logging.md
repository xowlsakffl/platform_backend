# Logging 운영 가이드

작성 기준: 2026-07-27

이 문서는 현재 코드 기준의 앱 로그, traceId, 감사 로그, 운영 히스토리, 큐/스케줄/Telescope 관측 로그를 정리한다.

기준 파일:

- `config/logging.php`
- `config/activitylog.php`
- `config/telescope.php`
- `config/horizon.php`
- `bootstrap/app.php`
- `routes/console.php`
- `app/Common/Http/Middleware/RequestId.php`
- `app/Common/Concerns/HasAuditLogs.php`
- `app/Domains/Common/OperationHistory`
- `app/Domains/Common/Notification/Actions/SendPushNotificationDeliveryAction.php`

## 1) 로그 분류

| 분류 | 저장 위치 | 목적 |
|---|---|---|
| 앱 로그 | `storage/logs/*` 또는 설정 채널 | 런타임 이벤트, 인증, 외부 API 오류, 예외 추적 |
| traceId | 앱 로그 context, 응답 header/body | 요청 단위 추적 |
| 감사 로그 | `activity_log` | 모델 변경 감사 추적 |
| 운영 히스토리 | `operation_histories`, `operation_history_changes` | Staff 화면에 보여줄 업무 처리 이력 |
| 큐 실패/배치 로그 | `failed_jobs`, `job_batches`, Horizon Redis | 비동기 작업 장애 분석 |
| 스케줄 모니터 로그 | `monitored_scheduled_tasks`, `monitored_scheduled_task_log_items` | Scheduler 실행/누락/실패 관측 |
| Telescope | `telescope_entries`, `telescope_entries_tags`, `telescope_monitoring` | 개발/운영 디버그 관측 |
| 일반 사용자 접속 기록 | `account_user_access_logs` | foreground 접속 기록 저장용 테이블 |

`activity_log`와 `operation_histories`는 목적이 다르다. 운영 화면에 보여줄 처리 이력은 `operation_histories`를 기준으로 하고, 모델 변경 감사 추적은 `activity_log`를 기준으로 한다.

## 2) Laravel 앱 로그

설정 파일:

- `config/logging.php`

기본 설정:

- `LOG_CHANNEL`: 기본값 `stack`
- `LOG_STACK`: 기본값 `single`
- `LOG_LEVEL`: 기본값 `debug`
- `LOG_DAILY_DAYS`: daily 채널 보관일, 기본값 14
- `LOG_DEPRECATIONS_CHANNEL`: deprecation 로그 채널, 기본값 `null`

주요 채널:

| 채널 | 용도 |
|---|---|
| `stack` | 여러 채널 묶음. `LOG_STACK` 쉼표 구분값 사용 |
| `single` | `storage/logs/laravel.log` 단일 파일 |
| `daily` | `storage/logs/laravel.log` base path 기준 일자별 rotation |
| `slack` | critical 이상 Slack webhook 전송 |
| `papertrail` | Papertrail UDP/TLS 전송 |
| `stderr` | 컨테이너/프로세스 stderr 출력 |
| `syslog` | OS syslog |
| `errorlog` | PHP error_log |
| `null` | 로그 폐기 |
| `emergency` | 일반 로그 채널 장애 시 fallback |

운영에서는 보통 `LOG_CHANNEL=stack`, `LOG_STACK=daily` 또는 외부 수집 채널 조합을 사용한다. 로컬/테스트에서는 `single`도 가능하다.

## 3) traceId

`RequestId` 미들웨어가 요청마다 traceId를 확정한다.

동작:

- 요청 헤더 `X-Request-Id`가 있으면 그대로 사용한다.
- 없으면 UUID를 생성한다.
- request attribute `traceId`에 저장한다.
- `Log::withContext(['traceId' => $traceId])`로 이후 로그 context에 넣는다.
- 응답 헤더 `X-Request-Id`로 내려준다.
- API 응답 body의 `traceId`에도 포함된다.

예외 context:

`bootstrap/app.php`는 예외 로그 context에 아래 값을 추가한다.

- `traceId`
- `path`
- `method`

주의:

- HTTP 요청에서만 `RequestId` 미들웨어가 동작한다.
- CLI, Queue, Scheduler 로그에는 요청 traceId가 없을 수 있다.
- 큐/스케줄 작업에서 연계 추적이 필요하면 job id, delivery id, target id 같은 별도 식별자를 context에 넣는다.

## 4) 현재 앱 로그 지점

현재 코드에서 `Log::info()`를 쓰는 주요 지점:

인증/계정:

- Staff 로그인 성공, 로그아웃, 프로필 수정, 비밀번호 변경
- Hospital 계정 로그인 성공, 로그아웃, 프로필 수정, 비밀번호 변경
- Beauty 계정 로그인 성공, 로그아웃, 프로필 수정, 비밀번호 변경
- 앱 사용자 로그인 성공, 로그아웃, 프로필 수정, 비밀번호 변경
- 내부도구 로그인, 로그아웃

비밀번호 재설정:

- 대상 없음 또는 비활성 계정에 대한 링크 요청
- 제한 시간 내 중복 링크 요청
- 비밀번호 재설정 링크 발송
- 비밀번호 재설정 완료

현재 코드에서 `Log::warning()`을 쓰는 주요 지점:

- 로그인 실패
- 내부도구 로그인 거부
- 현재 비밀번호 불일치로 인한 비밀번호 변경 실패
- 비밀번호 재설정 링크 검증 실패
- 푸시 발송 중 예외 발생
- FCM/APNs 푸시 발송 실패
- FCM access token 발급 실패

일반 목록/상세 조회, 생성, 수정, 삭제 성공 로그는 앱 로그에 남기지 않는다. 업무 처리 이력은 도메인별 운영 히스토리 또는 감사 로그에 남기고, 앱 로그는 인증/보안 이벤트, 외부 연동 실패, 예외, 큐/스케줄 장애 분석에 집중한다.

## 5) 민감정보 로깅 규칙

로그에 원문 그대로 남기지 않는 값:

- 비밀번호, 비밀번호 확인값
- access token, refresh token, reset token
- 메일 재설정 URL 전체
- 파일 객체, 파일 원본 내용
- private key, secret, authorization header
- 로그 직렬화를 깨뜨릴 수 있는 객체

허용하는 방식:

- 내부 추적용 ID: `account_id`, `hospital_id`, `beauty_id`, `delivery_id`
- 파일 객체: 파일명, MIME, 크기만 기록
- 비밀번호/토큰/secret 계열 key: `[REDACTED]`로 치환
- 일반 식별정보, 사업자번호, 병원명, 사람 이름, 검색어: 장애 분석에 필요한 경우에만 제한적으로 기록 가능

신규 코드 규칙:

- 목록/상세 조회 성공 로그는 추가하지 않는다.
- 생성/수정/삭제/상태 변경 성공 이력은 운영 히스토리 또는 감사 로그에 기록한다.
- 로그인/로그아웃/비밀번호 변경/계정정보 변경 같은 인증·보안 이벤트는 성공 후 `Log::info()`로 남긴다.
- 로그인 실패, 현재 비밀번호 불일치, 비밀번호 재설정 링크 검증 실패, 내부도구 접근 거부는 `Log::warning()`으로 남긴다.
- 실패 로그에는 `reason`을 넣고 비밀번호/토큰/재설정 URL 원문은 절대 넣지 않는다.
- raw FormRequest 전체, UploadedFile 객체, 비밀번호/토큰 계열 값을 직접 넣지 않는다.
- `Log::warning()`에 exception을 넣을 때는 class, message, 대상 id 위주로 제한한다.
- 외부 API 응답 body를 남기는 경우 토큰/개인정보가 포함되지 않는지 먼저 확인한다.

## 6) 감사 로그 `activity_log`

패키지:

- `spatie/laravel-activitylog`

설정:

- `config/activitylog.php`

주요 설정:

- `enabled`: `ACTIVITY_LOGGER_ENABLED`, 기본 true
- `delete_records_older_than_days`: 365
- `default_log_name`: `default`
- `table_name`: `activity_log`
- `database_connection`: `ACTIVITY_LOGGER_DB_CONNECTION`

공통 trait:

- `app/Common/Concerns/HasAuditLogs.php`

Trait 옵션:

- `useLogName('audit')`
- `logFillable()`
- `logOnlyDirty()`
- `dontSubmitEmptyLogs()`

현재 `HasAuditLogs` 적용 모델:

- `BeautyBusinessRegistration`
- `Category`
- `Hashtag`
- `HospitalBusinessRegistration`
- `HospitalDoctor`
- `HospitalEvaluation`
- `HospitalReview`
- `HospitalReviewComment`
- `HospitalReviewCommentMention`
- `Talk`
- `TalkComment`
- `TalkCommentMention`

현재 코드 기준 `activity('audit')` 직접 호출은 없다. 신규 직접 호출을 추가할 때는 자동 감사 로그와 중복 기록되지 않는지 먼저 확인한다.

## 7) 운영 히스토리

테이블:

- `operation_histories`
- `operation_history_changes`

목적:

- Staff 관리 화면에서 사람이 읽을 수 있는 처리 이력을 보여준다.
- 상태 변경, 검수 변경, 신고 처리, 영수증 인증, 관리자 메모 관련 변경 등 운영 행위를 표시한다.

기준:

- `./operation-history.md`

규칙:

- 저장은 `OperationHistoryCreateAction`을 사용한다.
- 변경값은 `operation_history_changes`에 필드별로 저장한다.
- 화면 표시값이 필요한 경우 `before_display`, `after_display`를 함께 저장한다.
- 단순 앱 로그로 운영 이력을 대체하지 않는다.

## 8) 큐 / Horizon 로그

큐 관련 저장소:

- `failed_jobs`: 실패 Job
- `job_batches`: batch 메타데이터
- Redis: Horizon 런타임 메타/메트릭

운영 도구:

- Horizon
- `php artisan queue:failed`
- `php artisan queue:retry`
- `php artisan queue:prune-failed`

현재 스케줄 정리:

- `queue:prune-batches --hours=72 --unfinished=72 --cancelled=168` 매일 03:10
- `queue:prune-failed --hours=168` 매일 03:20
- `horizon:snapshot` 5분마다

상세 기준:

- `./queue.md`

## 9) Scheduler / Schedule Monitor 로그

Spatie Schedule Monitor 테이블:

- `monitored_scheduled_tasks`
- `monitored_scheduled_task_log_items`

현재 `routes/console.php` 등록 스케줄:

- `schedule-monitor:sync` 매일 02:50
- `notice:cleanup-temp-editor-images --hours=24` 매시간
- `horizon:snapshot` 5분마다
- `queue:prune-batches --hours=72 --unfinished=72 --cancelled=168` 매일 03:10
- `queue:prune-failed --hours=168` 매일 03:20
- `hospital-evaluations:refresh-hospital-ratings` 매일 03:30

상세 기준:

- `./scheduler.md`

## 10) Telescope

설정:

- `config/telescope.php`

저장 테이블:

- `telescope_entries`
- `telescope_entries_tags`
- `telescope_monitoring`

현재 provider 동작:

- local 환경은 전체 기록
- local 외 환경은 reportable exception, failed request, failed job, scheduled task, monitored tag 중심으로 저장
- local 외 환경에서는 `_token`, cookie, CSRF header를 숨긴다.

운영 주의:

- `TELESCOPE_ENABLED=true`는 저장량이 커질 수 있다.
- 운영에서 켤 경우 민감 parameter/header 숨김 목록을 점검한다.
- Telescope는 디버그 관측 도구이지 장기 감사 로그 저장소가 아니다.

## 11) 일반 사용자 접속 기록

테이블/모델:

- `account_user_access_logs`
- `AccountUserAccessLog`

용도:

- 앱/웹이 foreground 상태로 열린 시점의 사용자 접속 기록 저장
- Staff 회원 상세에서 최근 접속 로그를 조회할 수 있는 구조

현재 코드 기준:

- 테이블, 모델, 조회 DTO/Query는 존재한다.
- `account_user_access_logs`를 생성하는 writer는 현재 코드에서 확인되지 않는다.

접속 로그 수집 기능을 추가할 때:

- API 요청마다 무조건 insert하지 않는다.
- foreground 진입, 앱 resume, 명시적 access ping 같은 제한된 이벤트에서 기록한다.
- `account_users.last_accessed_at`, `last_access_ip`와 함께 갱신한다.
- IP, user agent 저장 정책은 개인정보 보관 정책과 맞춘다.

## 12) 운영 조회 명령

앱 로그:

```bash
tail -f storage/logs/laravel.log
```

daily 채널 사용 시:

```bash
ls -lh storage/logs
tail -f storage/logs/laravel-$(date +%Y-%m-%d).log
```

큐:

```bash
php artisan horizon:status
php artisan queue:failed
php artisan queue:retry all
php artisan queue:prune-failed --hours=168
```

스케줄:

```bash
php artisan schedule:list
php artisan schedule-monitor:list
php artisan schedule-monitor:sync
```

감사 로그 예시:

```sql
SELECT id, log_name, event, description, subject_type, subject_id, causer_type, causer_id, created_at
FROM activity_log
ORDER BY id DESC
LIMIT 50;
```

운영 히스토리 예시:

```sql
SELECT id, target_type, target_id, actor_type, actor_id, action, reason, created_at
FROM operation_histories
ORDER BY id DESC
LIMIT 50;
```

## 13) 운영 원칙

- 장애 분석은 `traceId`로 앱 로그와 API 응답을 먼저 연결한다.
- 비동기 작업은 앱 로그만 보지 말고 `failed_jobs`, Horizon, `job_batches`를 같이 본다.
- 스케줄 장애는 `schedule:list`, `schedule-monitor:list`, `monitored_scheduled_task_log_items`를 같이 본다.
- 관리 화면에 보여줄 이력은 앱 로그가 아니라 운영 히스토리에 남긴다.
- 장기 감사 목적은 `activity_log` 또는 도메인 이력 테이블을 사용한다.
- 로그 보관/삭제 정책은 채널별로 분리해서 관리한다.
- 신규 로그를 추가할 때는 민감정보가 들어가는지 먼저 보고, 원본 payload 대신 최소 context만 남긴다.
