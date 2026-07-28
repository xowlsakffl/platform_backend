# Cache / Redis 적용 규칙

작성 기준: 2026-07-24

이 문서는 현재 코드 기준의 Cache, Redis, Rate Limit 적용 범위와 신규 캐시 추가 규칙을 정리한다.

Redis는 원본 저장소가 아니다. 돈, 승인상태, 광고 구좌, 신고 처리 상태 같은 최종 데이터는 DB transaction, row lock, unique/index 제약을 기준으로 확정한다. Cache/Redis는 조회 성능, 외부 토큰 재사용, rate limit 같은 보조 계층으로만 사용한다.

## 1) 기본 원칙

- 원본 데이터는 DB가 기준이다.
- 캐시는 반복 조회 비용을 줄이는 복사본이다.
- 조회성 캐시는 장애가 나도 API 전체 장애로 번지지 않게 fallback을 둔다.
- 데이터 변경 후에는 관련 캐시를 명시적으로 삭제하고, 다음 조회에서 다시 채운다.
- TTL은 무효화 누락을 완화하는 보조 장치다. TTL만 믿고 방치하지 않는다.
- 캐시 key는 도메인, 조건, 날짜처럼 결과를 바꾸는 값을 모두 포함한다.
- Cache와 Queue를 혼동하지 않는다. Queue 런타임은 `./queue.md`에서 관리한다.

## 2) 현재 Cache 설정

설정 파일:

- `config/cache.php`
- `config/database.php` Redis connection

현재 기본값:

- `CACHE_STORE`: 기본값 `database`
- `STAFF_SUMMARY_CACHE_STORE`: 기본값 `redis`
- `STAFF_SUMMARY_CACHE_TTL`: 기본값 `300`
- `HOSPITAL_EVENT_AD_CALENDAR_CACHE_STORE`: 기본값 `redis`
- `HOSPITAL_EVENT_AD_CALENDAR_CACHE_TTL`: 기본값 `60`

주의:

- Laravel `Cache::remember()`와 RateLimiter는 기본적으로 default cache store를 따른다.
- 운영에서 rate limit과 일반 cache를 Redis로 처리하려면 `.env`에서 `CACHE_STORE=redis`를 명시한다.
- Staff summary는 default store와 별개로 `STAFF_SUMMARY_CACHE_STORE`를 사용한다.
- 광고현황/광고 구좌 표시용 캐시는 default store와 별개로 `HOSPITAL_EVENT_AD_CALENDAR_CACHE_STORE`를 사용한다.

## 3) Staff Summary Cache

Staff 목록 상단 summary count는 `StaffSummaryCache`를 사용한다.

- 위치: `app/Domains/Common/Cache/Support/StaffSummaryCache.php`
- 기본 store: `STAFF_SUMMARY_CACHE_STORE=redis`
- 기본 TTL: `STAFF_SUMMARY_CACHE_TTL=300`
- 키 형식: `staff:summary:{domain}:{suffix}`
- suffix 기본값: `default`
- suffix 조건이 있으면 `parts` 배열을 `sha1(json_encode(array_values($parts)))`로 변환한다.
- Redis/cache 오류 시: 캐시를 건너뛰고 resolver DB 조회 결과를 반환한다.

현재 domain 상수:

- `account-user`
- `content-report`
- `hospital`
- `hospital-entry`
- `hospital-event`
- `hospital-video`

현재 적용된 summary:

- 일반회원 summary
- 병의원 summary
- 입점신청 summary
- 이벤트 summary
- 동영상 summary
- 신고게시물 summary

현재 광고 관리/광고 현황 summary는 `StaffSummaryCache` 대상이 아니다.

## 4) Summary Query 작성 규칙

Summary query는 아래 구조를 따른다.

```php
public function get(): array
{
    return StaffSummaryCache::remember(
        StaffSummaryCache::DOMAIN_HOSPITAL,
        fn (): array => $this->uncachedSummary(),
    );
}

private function uncachedSummary(): array
{
    // DB 집계 쿼리
}
```

규칙:

- `get()`은 캐시 진입점만 담당한다.
- 실제 DB 집계는 `uncachedSummary()`에 둔다.
- Controller/Action에서 summary count를 직접 만들지 않는다.
- 새로운 Staff summary API를 추가하면 `StaffSummaryCache`에 domain 상수를 먼저 추가한다.
- 필터별 summary를 캐시해야 하면 `parts`에 결과를 바꾸는 조건을 모두 넣는다.
- 날짜 기준 summary는 날짜를 `parts`에 포함한다.

## 5) Content Report Summary Cache

신고게시물 summary는 `ContentReportSummaryCache`가 `StaffSummaryCache`를 감싼다.

- 위치: `app/Domains/Common/ContentReport/Support/ContentReportSummaryCache.php`
- domain: `StaffSummaryCache::DOMAIN_CONTENT_REPORT`
- parts: `targetClass`, `categoryDomain 또는 all`, `today()`

의미:

- 신고게시물 summary는 대상 타입별로 분리한다.
- 성형후기/쁘띠후기처럼 category domain이 있는 대상은 category domain까지 key에 포함한다.
- `today()`가 key에 들어가므로 날짜가 바뀌면 새 key를 사용한다.

무효화:

- `forgetForTarget($target)`을 사용한다.
- HospitalVideo 신고 상태가 바뀌면 신고게시물 summary와 동영상 summary를 같이 무효화한다.

## 6) 현재 무효화 기준

상태/카운트에 영향을 주는 write action 완료 후 해당 summary 캐시를 삭제한다.

예:

```php
$updated = DB::transaction(function () {
    // 원본 데이터 변경
});

StaffSummaryCache::forget(StaffSummaryCache::DOMAIN_HOSPITAL);
```

현재 연결된 무효화 기준:

- 일반회원: 차단/정상 상태 변경, 신고 경고 처리로 `warning_count` 또는 차단 상태가 바뀌는 경우
- 병의원: 생성, 삭제, 검수상태 변경, 병의원상태 변경, 수정, 병원 계정 로그인으로 휴면 여부가 바뀌는 경우
- 입점신청: 검수상태 변경, 수정 payload에 `allow_status`가 포함된 경우
- 이벤트: 생성, 복제, 삭제, 검수상태 변경, 강제중지 변경, 기간 변경, 공개여부/검수/기간 관련 수정
- 동영상: 생성, 삭제, 강제중지 변경, 병원 공개여부 변경, 공개여부/강제중지 관련 수정, 동영상 신고 처리 상태 변경
- 신고게시물: 신고 생성, 신고 처리, 신고 대상 상태 변경, 경고여부 변경

원칙:

- write action에서 summary 조건에 영향을 주는 필드를 변경하면 반드시 `forget()`을 호출한다.
- DB transaction이 있는 경우 원본 변경이 끝난 뒤 캐시를 지운다.
- 캐시 삭제 실패는 원본 변경 실패로 처리하지 않는다. `StaffSummaryCache`는 내부에서 예외를 삼키고 계속 진행한다.

## 7) Rate Limit

RateLimiter 정의 위치:

- `app/Providers/RateLimitServiceProvider.php`

현재 적용된 limiter:

- `auth-login`
- `password-reset-link`
- `password-reset-verify`
- `password-reset-submit`
- `password-update`
- `content-report-create`

적용 라우트:

- Staff/Hospital/Beauty/User 로그인
- Staff/Hospital/Beauty/User 비밀번호 재설정 링크 발송/검증/제출
- Staff/Hospital/Beauty/User 로그인 후 비밀번호 변경
- User 신고 생성

RateLimiter는 Laravel cache store를 사용한다. 운영에서 다중 서버로 갈 경우 file/database cache보다 Redis가 적합하다. 다만 Redis가 rate limit의 원본이라는 뜻은 아니다. 제한 기준은 limiter key와 window이고, 저장소는 그 상태를 빠르게 공유하기 위한 수단이다.

## 8) 광고현황/구좌 표시 Cache

광고현황 달력과 광고 등록 2단계 구좌 조회는 `HospitalEventAdCalendarCache`를 사용한다.

- 위치: `app/Domains/HospitalEventAd/Support/HospitalEventAdCalendarCache.php`
- 기본 store: `HOSPITAL_EVENT_AD_CALENDAR_CACHE_STORE=redis`
- 기본 TTL: `HOSPITAL_EVENT_AD_CALENDAR_CACHE_TTL=60`
- 캐시 대상: 광고현황 달력, 광고 구좌 availability 응답
- 캐시 역할: 화면 표시용 조회 가속
- 최종 검증 기준: DB transaction, row lock, 구좌 count 재검증

키 구조:

- `hospital-event-ad:calendar:{version}:{hash}`
- `hospital-event-ad:availability:{version}:{hash}`

hash parts:

- calendar: `group`, `category_id 또는 all`, `month`, `today`
- availability: `placement`, `category_id 또는 none`, `month`, `today`

무효화 방식:

- `HospitalEventAdCalendarCache::flush()`가 version key를 갱신한다.
- version이 바뀌면 기존 calendar/availability key는 읽히지 않고 TTL 뒤 자연 만료된다.
- tag cache를 쓰지 않기 때문에 Redis 외 store에서도 동일한 방식으로 동작한다.

현재 무효화 지점:

- 광고 등록
- 광고 수정
- 광고 검수상태 변경

주의:

- 광고 승인/수정/등록 최종 검증은 캐시를 보지 않는다.
- 승인 시 병의원 상태, 이벤트 상태, 광고 이미지, 구좌 수는 DB에서 다시 확인한다.
- 구좌 3개 초과 방지는 Redis가 아니라 DB 기준으로 처리한다.

## 9) 외부 토큰 Cache

현재 `Cache::remember()` 직접 사용:

- FCM access token: `notification_push:fcm_access_token:{hash}`, 55분
- APNs provider token: `notification_push:apns_provider_token:{hash}`, 50분

위치는 `app/Domains/Common/Notification/Actions/SendPushNotificationDeliveryAction.php`다.

주의:

- 이 캐시는 default cache store를 사용한다.
- token 원본은 외부 provider가 발급하는 값이다.
- cache miss 시 외부 provider에 다시 요청한다.
- Push 발송은 큐 작업으로 처리되며, Queue 자체 규칙은 `queue.md`를 따른다.

## 10) 현재 Redis Lock 적용 상태

현재 코드 기준 Redis Lock은 적용되어 있지 않다.

광고 구좌 승인/수정 충돌 방지는 Redis Lock이 아니라 DB transaction과 `lockForUpdate()`로 처리한다.

- `HospitalEventAdStateUpdateForStaffQuery::getForUpdate()`
- 승인 시 병의원/이벤트/광고이미지/구좌를 다시 검증
- 구좌 count는 `HospitalEventAdSlotAvailabilityForStaffQuery`가 DB에서 계산

돈, 구좌, 승인 같은 최종 정합성은 Redis Lock만으로 해결하지 않는다. Redis Lock을 추가하더라도 DB transaction과 제약 조건이 최종 방어선이어야 한다.

## 11) Redis Cache 적용 판단

현재 적용:

- Staff summary count
- 신고게시물 summary count
- 광고현황 달력/광고 구좌 availability 표시용 cache
- FCM/APNs token cache
- Rate limit 저장소로 사용 가능

적용 후보:

- 카테고리/selector/options
- 72시간 동영상 신고오류 재신고 제한 보조 cache

부적합:

- 충전금 원장
- 광고 구좌 최종 확정
- 승인/반려 최종 상태
- 운영 히스토리 원본
- 신고 원본/처리 상태 원본

현재 72시간 동영상 신고오류 재신고 제한은 Redis가 아니라 `ContentReportState::normal_visible_at`과 `VIDEO_NORMAL_VISIBLE_REOPEN_LOCK_HOURS` 기준으로 판단한다. Redis를 추가하더라도 DB 상태가 기준이어야 한다.

## 12) 신규 캐시 추가 체크리스트

1. 캐시 대상이 원본 데이터가 아니라 조회 가속인지 확인한다.
2. 결과를 바꾸는 조건을 key parts에 모두 넣는다.
3. TTL을 정한다.
4. write action에서 무효화 지점을 정한다.
5. 캐시 장애 시 fallback 정책을 정한다.
6. 캐시 store를 default로 둘지 별도 config로 분리할지 정한다.
7. 문서에 적용 대상, key 구조, 무효화 기준을 추가한다.

신규 Staff summary라면 `StaffSummaryCache`를 우선 사용한다. 도메인 전용 캐시가 필요하면 `app/Domains/{Domain}/Support` 또는 공통 성격이면 `app/Domains/Common/{Feature}/Support`에 둔다.
