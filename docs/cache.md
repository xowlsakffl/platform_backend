# Cache / Redis 적용 규칙

작성 기준: 2026-07-28

이 문서는 Spring Boot 백엔드의 Cache, Redis, Rate Limit 적용 기준을 정의한다.

Redis는 원본 저장소가 아니다. 돈, 승인상태, 광고 구좌, 신고 처리 상태 같은 최종 데이터는 DB transaction, row lock, unique/index 제약으로 확정한다. Cache/Redis는 조회 성능, 외부 토큰 재사용, rate limit 같은 보조 계층으로만 사용한다.

## 1. 기본 원칙

- 원본 데이터는 DB가 기준이다.
- 캐시는 반복 조회 비용을 줄이는 복사본이다.
- 조회성 캐시는 장애가 나도 API 전체 장애로 번지지 않게 fallback을 둔다.
- 데이터 변경 후에는 관련 캐시를 명시적으로 삭제한다.
- TTL은 무효화 누락을 완화하는 보조 장치다.
- 캐시 key는 도메인, 조건, 날짜처럼 결과를 바꾸는 값을 모두 포함한다.
- Cache와 Queue를 혼동하지 않는다. Queue 런타임은 [Queue 문서](./queue.md)에서 관리한다.

## 2. 설정 기준

현재 로컬 Redis 기준:

```text
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=myStrongRedisPassword
```

Spring 설정 위치:

- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/java/com/medi/infrastructure/redis`
- `src/main/java/com/medi/common/config`

Redis repository 자동 스캔은 사용하지 않는다. Redis는 cache, lock, queue 같은 명시적 adapter에서만 사용한다.

## 3. Staff Summary Cache

Staff 목록 상단 summary count는 캐시 후보가 된다.

권장 구조:

```text
application/{domain}
  -> Summary 조회 Service

infrastructure/redis
  -> StaffSummaryCache adapter
```

규칙:

- Service는 먼저 cache adapter를 통해 조회한다.
- cache miss면 DB summary query를 실행한다.
- DB 조회 결과를 TTL과 함께 캐시에 저장한다.
- Redis 장애가 발생하면 DB 조회로 fallback한다.
- 생성/수정/삭제/상태 변경 유스케이스는 관련 summary cache를 무효화한다.

권장 key 형식:

```text
staff:summary:{domain}:{hash}
```

`hash`에는 결과를 바꾸는 조건을 모두 포함한다.

## 4. Summary Query 작성 규칙

Summary 조회는 아래 책임을 분리한다.

- Service: cache 조회, cache miss fallback, cache 저장
- Repository: DB 집계 쿼리
- Controller: Service 호출과 응답 반환

Controller에서 summary count를 직접 만들지 않는다.

## 5. Rate Limit

Rate limit은 인증, 비밀번호 재설정, 신고 생성처럼 남용 가능한 API에 적용한다.

초기 후보:

- `auth-login`
- `password-reset-link`
- `password-reset-verify`
- `password-reset-submit`
- `password-update`
- `content-report-create`

다중 서버 운영에서는 Redis 기반 rate limit 저장소가 적합하다. 단, Redis는 제한 상태를 공유하는 저장소일 뿐이고, 보안 정책 자체는 application/security 계층에서 정의한다.

## 6. 광고현황/구좌 표시 Cache

광고현황 달력과 광고 구좌 availability는 캐시 후보가 된다.

권장 key:

```text
hospital-event-ad:calendar:{version}:{hash}
hospital-event-ad:availability:{version}:{hash}
```

규칙:

- 화면 표시용 조회 가속에만 사용한다.
- 승인/수정/등록 최종 검증은 캐시를 보지 않는다.
- 승인 시 병원 상태, 이벤트 상태, 광고 이미지, 구좌 수는 DB에서 다시 확인한다.
- 구좌 초과 방지는 Redis가 아니라 DB transaction과 제약 조건으로 처리한다.

## 7. 외부 토큰 Cache

FCM, APNs 같은 외부 provider token은 Redis 캐시 후보다.

권장 key:

```text
notification:push:fcm-access-token:{hash}
notification:push:apns-provider-token:{hash}
```

규칙:

- token 원본은 외부 provider가 발급하는 값이다.
- cache miss 시 외부 provider에 다시 요청한다.
- provider 오류는 알림 delivery 실패로 기록하고 원본 업무를 롤백하지 않는다.

## 8. Redis Lock 판단 기준

Redis lock은 최종 정합성 수단이 아니다.

사용해도 되는 경우:

- 중복 실행 비용을 줄이는 작업
- 짧은 시간의 중복 스케줄러 실행 방지
- 외부 API 호출 폭주 완화

DB 기준으로 막아야 하는 경우:

- 결제/잔액
- 승인 상태
- 광고 구좌
- 중복 신청
- 신고 처리 상태

Redis lock을 추가하더라도 DB transaction과 unique/index 제약이 최종 방어선이어야 한다.

## 9. 신규 Cache 추가 체크리스트

- 원본 데이터가 DB에 있는가?
- cache miss 시 DB로 복구 가능한가?
- 결과를 바꾸는 조건을 key에 모두 넣었는가?
- 데이터 변경 지점에서 무효화하는가?
- TTL이 설정되어 있는가?
- Redis 장애 시 API가 실패해야 하는 기능인가, fallback 가능한 기능인가?
- 개인정보나 민감정보를 key/value에 넣지 않았는가?
