# Error Handling

작성 기준: 2026-07-27

이 문서는 현재 코드 기준의 공통 예외 처리, 에러 응답 포맷, traceId, rate limit 처리 규칙을 정리한다.

기준 파일:

- `bootstrap/app.php`
- `app/Common/Exceptions/ErrorCode.php`
- `app/Common/Exceptions/CustomException.php`
- `app/Common/Http/Responses/ApiResponse.php`
- `app/Common/Http/Middleware/RequestId.php`
- `app/Providers/RateLimitServiceProvider.php`

## 1) 적용 범위

JSON 예외 응답은 `bootstrap/app.php`의 공통 exception renderer가 만든다.

현재 JSON으로 렌더링되는 요청:

- URL이 `api/*`인 요청
- `expectsJson()`이 true인 요청

Staff / Hospital / Beauty / User API는 `/api/v1/...` 하위에 있으므로 공통 JSON 에러 응답을 따른다.

Horizon, Telescope, Scramble, 내부 도구 같은 비 API 웹 화면은 이 문서의 JSON 응답 규칙 대상이 아니다. 해당 화면은 웹 세션 guard와 Laravel의 일반 redirect/error page 흐름을 따른다.

## 2) 에러 응답 포맷

모든 API 에러 응답은 `ApiResponse::errorCode()` 또는 `ApiResponse::error()`를 통해 아래 구조로 반환한다.

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다."
  },
  "traceId": "request-trace-id"
}
```

규칙:

- `success`: 항상 `false`
- `error.code`: `App\Common\Exceptions\ErrorCode` 값
- `error.message`: 클라이언트에 노출 가능한 메시지
- `error.details`: 상세 정보가 있을 때만 포함
- `traceId`: 요청 추적 ID

Validation 실패는 `error.details.errors`에 Laravel validation error 배열을 담는다.

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "details": {
      "errors": {
        "name": ["이름은 필수 항목입니다."]
      }
    }
  },
  "traceId": "request-trace-id"
}
```

성공 응답 포맷은 `./api-response.md`를 따른다.

## 3) traceId 정책

`RequestId` 미들웨어는 모든 요청 앞단에서 traceId를 확정한다.

동작:

- 요청 헤더 `X-Request-Id`가 있으면 해당 값을 사용한다.
- 없으면 서버에서 UUID를 생성한다.
- 확정된 값은 request attribute `traceId`에 저장한다.
- 응답 헤더 `X-Request-Id`로 다시 내려준다.
- 로그 컨텍스트에 `traceId`를 넣는다.

`bootstrap/app.php`의 exception context에는 아래 값이 들어간다.

- `traceId`
- `path`
- `method`

운영에서 장애를 볼 때는 클라이언트가 받은 `traceId`와 서버 로그의 `traceId`를 기준으로 추적한다.

## 4) ErrorCode

현재 공통 ErrorCode는 아래와 같다.

| ErrorCode | HTTP | 기본 메시지 |
|---|---:|---|
| `INTERNAL_ERROR` | 500 | 서버 오류가 발생했습니다. |
| `INVALID_REQUEST` | 422 | 요청 값이 올바르지 않습니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `FORBIDDEN` | 403 | 권한이 없습니다. |
| `NOT_FOUND` | 404 | 요청한 정보를 찾을 수 없습니다. |
| `METHOD_NOT_ALLOWED` | 405 | 허용되지 않는 HTTP 메서드입니다. |
| `TOKEN_ERROR` | 419 | 토큰이 유효하지 않습니다. |
| `DB_ERROR` | 500 | 데이터베이스 오류가 발생했습니다. |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다. |
| `RATE_LIMITED` | 429 | 요청이 너무 많습니다. 잠시 후 다시 시도해주세요. |
| `PAYLOAD_TOO_LARGE` | 413 | 요청 용량이 초과되었습니다. |

신규 에러 코드는 문자열을 임의로 내려주지 말고 `ErrorCode` enum에 추가한다. 단, 도메인별 상태값이나 validation message를 ErrorCode로 과도하게 쪼개지 않는다.

## 5) 예외 매핑

`bootstrap/app.php`는 예외를 아래 기준으로 `ApiResponse`에 매핑한다.

| 예외 | ErrorCode | HTTP | details |
|---|---|---:|---|
| `CustomException` | exception이 가진 ErrorCode | ErrorCode 기준 | exception details가 있으면 포함 |
| `ValidationException` | `INVALID_REQUEST` | 422 | `errors` 포함 |
| `AuthenticationException` | `UNAUTHORIZED` | 401 | 없음 |
| Spatie `UnauthorizedException` | `FORBIDDEN` | 403 | 없음 |
| `AuthorizationException` | `FORBIDDEN` | 403 | 없음 |
| `ModelNotFoundException` | `NOT_FOUND` | 404 | 없음 |
| `NotFoundHttpException` | `NOT_FOUND` | 404 | 없음 |
| `MethodNotAllowedHttpException` | `METHOD_NOT_ALLOWED` | 405 | 없음 |
| `PostTooLargeException` | `PAYLOAD_TOO_LARGE` | 413 | 없음 |
| `QueryException` | `DB_ERROR` | 500 | `APP_DEBUG=true`일 때만 sql/bindings |
| `HttpExceptionInterface` 401 | `UNAUTHORIZED` | 401 | 없음 |
| `HttpExceptionInterface` 403 | `FORBIDDEN` | 403 | 없음 |
| `HttpExceptionInterface` 404 | `NOT_FOUND` | 404 | 없음 |
| `HttpExceptionInterface` 405 | `METHOD_NOT_ALLOWED` | 405 | 없음 |
| `HttpExceptionInterface` 419 | `TOKEN_ERROR` | 419 | 없음 |
| `HttpExceptionInterface` 422 | `INVALID_REQUEST` | 422 | 없음 |
| `HttpExceptionInterface` 429 | `RATE_LIMITED` | 429 | 없음 |
| 기타 `HttpExceptionInterface` | `INTERNAL_ERROR` | 실제 HTTP status | exception message 사용 가능 |
| 기타 `Throwable` | `INTERNAL_ERROR` | 500 | `APP_DEBUG=true`일 때만 exception/message |

Laravel throttle 예외는 HTTP 429 예외로 들어오므로 `RATE_LIMITED`로 응답한다.

## 6) CustomException 사용 규칙

`CustomException`은 validation rule만으로 표현하기 어려운 비즈니스 규칙 위반에 사용한다.

예:

- 이미 신고한 콘텐츠
- 승인할 수 없는 광고 상태
- 선택한 병의원에 소속되지 않은 의료진
- 정책상 변경할 수 없는 상태

사용 방식:

```php
throw new CustomException(ErrorCode::INVALID_REQUEST, '광고 이미지를 등록해 주세요.');
```

메시지를 넘기지 않으면 `ErrorCode::messageApp()` 기본 메시지를 사용한다.

```php
throw new CustomException(ErrorCode::USER_NOT_FOUND);
```

규칙:

- FormRequest에서 처리 가능한 필수값, 타입, 범위 검증은 FormRequest에 둔다.
- DB 조회, 상태 전이, 중복 처리 같은 도메인 규칙은 Action 또는 Query에서 `CustomException`으로 처리한다.
- Controller에서 도메인 규칙을 직접 만들지 않는다. 단순 라우트 중첩 리소스 소유관계 확인처럼 라우팅 보정 성격의 검증만 예외적으로 허용한다.
- Controller에서 `try/catch`로 에러 응답 JSON을 직접 만들지 않는다.

## 7) 디버그 정보 노출

현재 코드는 debug details 노출을 `config('app.debug')`로만 제어한다.

`APP_DEBUG=false`:

- `QueryException` details 없음
- 기타 `Throwable` details 없음

`APP_DEBUG=true`:

- `QueryException`: `sql`, `bindings` 포함
- 기타 `Throwable`: `exception`, `message` 포함

현재 구현에는 Actor별 debug details 분기가 없다. 운영 환경에서는 반드시 `APP_DEBUG=false`로 둬야 한다.

## 8) Rate Limit

Rate limiter 정의는 `RateLimitServiceProvider`에 둔다.

현재 정의된 limiter:

| 이름 | 기준 | 제한 |
|---|---|---|
| `auth-login` | actor + login identifier + IP | 분당 6회 |
| `auth-login` | actor + IP | 분당 30회 |
| `password-reset-link` | actor + email 또는 IP | 분당 3회 |
| `password-reset-link` | actor + IP | 분당 10회 |
| `password-reset-verify` | actor + email 또는 IP | 분당 20회 |
| `password-reset-submit` | actor + email 또는 IP | 분당 6회 |
| `password-reset-submit` | actor + IP | 분당 20회 |
| `password-update` | actor + 로그인 사용자 또는 IP | 분당 6회 |
| `password-update` | actor + IP | 분당 20회 |
| `content-report-create` | user id 또는 IP | 분당 10회 |
| `content-report-create` | IP | 분당 60회 |

라우트에 적용된 limiter:

- 로그인
- 비밀번호 재설정 링크 발송
- 비밀번호 재설정 링크 검증
- 비밀번호 재설정 제출
- 로그인 사용자 비밀번호 변경
- 사용자 신고 생성

추가 정책:

- 인증/보안성 API는 신규 추가 시 기본적으로 rate limit을 붙인다.
- 신고 생성처럼 남용 가능성이 있는 API는 사용자 기준과 IP 기준을 같이 둔다.
- rate limit 저장소는 Laravel cache store를 따른다. 운영에서는 Redis cache 사용이 적합하다.

## 9) 인증 실패와 redirect

API 요청은 공통 예외 핸들러에서 `UNAUTHORIZED` JSON으로 응답한다.

비 API 웹 화면의 guest redirect는 `bootstrap/app.php`의 `redirectGuestsTo` 설정을 따른다.

- Horizon / Telescope / Scramble 등 내부 도구는 `tool_staff.login`으로 보낸다.
- API guard 로그인 라우트가 있는 경우 해당 guard login route를 사용할 수 있다.
- API JSON 에러 응답 정책과 웹 redirect 정책은 별개로 본다.

## 10) 신규 코드 작성 규칙

신규 코드에서 지킬 기준:

- 에러 응답 포맷은 Controller마다 만들지 않는다.
- `ApiResponse::error()` 직접 호출은 지양하고, 일반적으로 `ErrorCode` 기반의 `ApiResponse::errorCode()` 또는 예외를 사용한다.
- 도메인 실패는 `CustomException`으로 올리고 공통 핸들러가 변환하게 한다.
- validation 실패는 FormRequest 또는 Validator가 `ValidationException`으로 처리하게 둔다.
- 인증/권한 실패는 guard, middleware, policy, Gate가 던지는 예외를 그대로 사용한다.
- 파일 업로드 용량 초과는 `PostTooLargeException` 매핑을 따른다.
- traceId를 Controller나 Action에서 직접 만들지 않는다.
- 운영 응답에 SQL, token, 개인정보, 내부 exception message를 노출하지 않는다.

## 11) 비동기 오류

이 문서는 HTTP API 응답 기준이다.

Queue Job, Scheduler, 외부 API 후처리 실패는 HTTP 응답 포맷으로 표현되지 않을 수 있다.

운영 확인 위치:

- Queue 실패: `failed_jobs`, Horizon
- Scheduler 실패/누락: `monitored_scheduled_tasks`, `monitored_scheduled_task_log_items`
- 런타임 로그: `storage/logs/laravel.log`

API가 200을 반환했더라도 큐 기반 후속 작업은 별도로 실패할 수 있으므로, 비동기 작업은 queue/scheduler 문서 기준으로 모니터링한다.
