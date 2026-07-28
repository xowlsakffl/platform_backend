# Scheduler 운영 가이드

작성 기준: 2026-07-27

이 문서는 Beaulab 프로젝트의 스케줄 실행 구조를 정리한다.

큐/Horizon 상세는 [queue.md](./queue.md)에서 관리한다.

## 1) 실행 구조

운영 스케줄은 세 단계로 실행된다.

1. OS `crontab`이 매분 `php artisan schedule:run` 실행
2. Laravel Scheduler가 `routes/console.php`에 등록된 작업 중 due 상태인 작업 실행
3. Spatie Schedule Monitor가 실행 결과를 DB에 기록

역할 구분:

| 구성 | 역할 |
|---|---|
| OS crontab | Laravel Scheduler를 매분 깨우는 트리거 |
| Laravel Scheduler | 실행할 명령과 주기 정의 |
| Schedule Monitor | 스케줄 등록/실행/실패/누락 감시 |
| Horizon | 스케줄러가 발행한 Queue Job 실행 |

스케줄러는 시간 기반 트리거만 담당한다. 오래 걸리는 작업이나 외부 API 반복 호출은 Scheduler에서 직접 오래 실행하지 말고 Queue Job으로 넘기는 구조를 우선한다.

## 2) 현재 등록 스케줄

`routes/console.php` 기준:

| 명령 | 주기 | 목적 |
|---|---|---|
| `schedule-monitor:sync` | 매일 02:50 | Schedule Monitor 대상 동기화 |
| `notice:cleanup-temp-editor-images --hours=24` | 매시간 | 공지 에디터 임시 이미지 정리 |
| `horizon:snapshot` | 5분마다 | Horizon 메트릭 스냅샷 수집 |
| `queue:prune-batches --hours=72 --unfinished=72 --cancelled=168` | 매일 03:10 | 오래된 job batch 메타 정리 |
| `queue:prune-failed --hours=168` | 매일 03:20 | 오래된 failed job 정리 |
| `hospital-evaluations:refresh-hospital-ratings` | 매일 03:30 | 병원별 평가 수/평균 평점 집계 보정 |

## 3) 수동 실행 Artisan 명령

`routes/console.php`에는 스케줄 등록 외에도 수동 실행용 명령이 있다.

| 명령 | 용도 | 스케줄 등록 |
|---|---|---:|
| `notice:cleanup-temp-editor-images {--hours=24}` | 오래된 공지 에디터 임시 이미지 정리 | 예 |
| `notifications:send-pending-push {--limit=100}` | 누락된 pending Push delivery 재큐잉 | 아니오 |
| `media:generate-variants {--force} {--limit=500}` | 기존 이미지 thumb/medium variant 백필 | 아니오 |
| `hospital-evaluations:refresh-hospital-ratings {--hospital-id=*}` | 병원 평가 평균/집계 보정 | 예 |

수동 명령을 스케줄에 새로 올릴 때는 이 문서의 체크리스트를 따른다.

## 4) Schedule Monitor

패키지:

- `spatie/laravel-schedule-monitor`

현재 프로젝트는 별도 `config/schedule-monitor.php` 파일을 두지 않고 패키지 기본 설정과 마이그레이션 테이블을 사용한다.

테이블:

| 테이블 | 역할 |
|---|---|
| `monitored_scheduled_tasks` | 모니터링 대상 스케줄 레지스트리 |
| `monitored_scheduled_task_log_items` | 실행 이벤트 로그 |

스케줄을 추가/수정/삭제한 뒤에는 반드시 동기화한다.

```bash
php artisan schedule-monitor:sync
```

확인 명령:

```bash
php artisan schedule:list
php artisan schedule-monitor:list
```

## 5) 서버 운영 기준

운영 서버 crontab 예시:

```cron
* * * * * cd /root/beaulab && php artisan schedule:run >> /dev/null 2>&1
```

로컬 개발 중에는 아래 중 하나를 쓴다.

```bash
php artisan schedule:work
```

또는 단발 확인:

```bash
php artisan schedule:run
```

WSL/로컬 환경에서 systemd가 없는 경우에도 Laravel Scheduler 자체는 `schedule:work` 또는 수동 `schedule:run`으로 확인할 수 있다.

## 6) 배포 체크리스트

1. 코드 배포
2. `php artisan migrate --force`
3. 스케줄 변경이 있으면 `php artisan schedule-monitor:sync`
4. `php artisan schedule:list` 확인
5. `php artisan schedule-monitor:list` 확인
6. Horizon 관련 스케줄/큐 변경이 있으면 `php artisan horizon:terminate`

스케줄 변경이 없더라도 신규 배포 후 한 번은 `schedule:list`로 등록 상태를 확인한다.

## 7) 신규 스케줄 작성 규칙

1. 등록 위치는 `routes/console.php`로 통일한다.
2. 스케줄 이름/목적을 주석으로 남긴다.
3. 장시간 실행 가능성이 있으면 Queue Job으로 분리한다.
4. 중복 실행되면 안 되는 작업은 `withoutOverlapping()` 적용을 검토한다.
5. 다중 서버 운영 시 한 서버에서만 실행해야 하는 작업은 `onOneServer()` 적용을 검토한다.
6. 스케줄 추가 후 `schedule-monitor:sync`를 실행한다.
7. `./scheduler.md`와 메인 `README.md`의 요약 표를 갱신한다.

현재 단일 서버 기준이라 `onOneServer()`는 필수는 아니다. 다중 서버로 확장하면 정산, 집계, 정리 작업부터 우선 적용한다.

## 8) 장애 대응

### 스케줄이 전혀 실행되지 않음

1. crontab 등록 여부 확인
2. crontab의 프로젝트 경로 확인
3. PHP binary와 `.env` 로딩 확인
4. `php artisan schedule:run -v` 단독 실행
5. 서버 시간대와 `APP_TIMEZONE` 확인

### 특정 작업만 누락

1. `php artisan schedule:list`에서 Next Due 확인
2. `php artisan schedule-monitor:list`에서 상태 확인
3. 해당 명령을 단독 실행해 예외 확인
4. Queue를 발행하는 명령이면 Horizon 상태 확인

### 병원 평가 평균 집계가 맞지 않음

단일 병원 보정:

```bash
php artisan hospital-evaluations:refresh-hospital-ratings --hospital-id=1
```

전체 병원 보정:

```bash
php artisan hospital-evaluations:refresh-hospital-ratings
```

이 명령은 평가별 `average_rating`을 먼저 재계산한 뒤 병원별 `evaluation_count`, `evaluation_average_rating`을 다시 집계한다.

### Horizon 그래프가 비어 있음

1. `horizon:snapshot` 스케줄 등록 확인
2. `php artisan horizon:snapshot` 단독 실행
3. Horizon Redis 연결 확인
4. `config/horizon.php`의 `metrics.trim_snapshots` 확인
