# 에러 처리

작성 기준: 2026-07-28

이 문서는 Spring Boot 백엔드의 공통 예외 처리, 에러 응답 포맷, traceId 규칙을 정의한다.

## 기준 파일

- `src/main/java/com/medi/common/error/ErrorCode.java`
- `src/main/java/com/medi/common/error/ApiException.java`
- `src/main/java/com/medi/common/error/GlobalExceptionHandler.java`
- `src/main/java/com/medi/common/security/ApiSecurityExceptionHandler.java`
- `src/main/java/com/medi/common/web/ApiError.java`
- `src/main/java/com/medi/common/web/ApiResponse.java`
- `src/main/java/com/medi/common/web/RequestTraceFilter.java`
- `src/main/java/com/medi/common/web/RequestTrace.java`

## 1. 적용 범위

Staff, Hospital, Beauty, User, Public API는 모두 공통 JSON 에러 응답을 따른다.

```text
/api/v1/public
/api/v1/staff
/api/v1/hospital
/api/v1/beauty
/api/v1/user
```

Controller에서 직접 에러 JSON을 만들지 않는다. 예외를 던지고 공통 핸들러가 응답으로 변환한다.

## 2. 에러 응답 포맷

모든 API 에러 응답은 아래 구조를 사용한다.

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

상세 정보가 있으면 `error.details`를 포함한다.

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "details": {
      "errors": {
        "name": ["필수 값입니다."]
      }
    }
  },
  "traceId": "request-trace-id"
}
```

성공 응답은 [API 응답 문서](./api-response.md)를 따른다.

## 3. ErrorCode

현재 공통 `ErrorCode`는 아래와 같다.

| ErrorCode | HTTP | 기본 메시지 |
| --- | ---: | --- |
| `INVALID_REQUEST` | 400 | 요청 값이 올바르지 않습니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. |
| `NOT_FOUND` | 404 | 요청한 리소스를 찾을 수 없습니다. |
| `METHOD_NOT_ALLOWED` | 405 | 허용되지 않은 요청 방식입니다. |
| `CONFLICT` | 409 | 요청이 현재 리소스 상태와 충돌합니다. |
| `PAYLOAD_TOO_LARGE` | 413 | 요청 본문 크기가 너무 큽니다. |
| `INTERNAL_ERROR` | 500 | 서버 오류가 발생했습니다. |

신규 에러 코드는 문자열을 임의로 내려주지 말고 `ErrorCode` enum에 추가한다.

## 4. 예외 매핑

`GlobalExceptionHandler`는 예외를 아래 기준으로 변환한다.

| 예외 | ErrorCode | HTTP |
| --- | --- | ---: |
| `ApiException` | 예외가 가진 `ErrorCode` | `ErrorCode` 기준 |
| `MethodArgumentNotValidException` | `INVALID_REQUEST` | 400 |
| `ConstraintViolationException` | `INVALID_REQUEST` | 400 |
| `HttpMessageNotReadableException` | `INVALID_REQUEST` | 400 |
| `MissingServletRequestParameterException` | `INVALID_REQUEST` | 400 |
| `HttpRequestMethodNotSupportedException` | `METHOD_NOT_ALLOWED` | 405 |
| `NoHandlerFoundException` | `NOT_FOUND` | 404 |
| `AccessDeniedException` | `FORBIDDEN` | 403 |
| 그 외 `Exception` | `INTERNAL_ERROR` | 500 |

인증 실패와 권한 실패는 Spring Security의 `AuthenticationEntryPoint`, `AccessDeniedHandler`에서 공통 응답으로 내려준다.

## 5. ApiException 사용 기준

`ApiException`은 Bean Validation만으로 표현하기 어려운 비즈니스 규칙 위반에 사용한다.

예:

```java
throw new ApiException(ErrorCode.CONFLICT, "이미 사용 중인 병원명입니다.");
```

기준:

- 필수값, 타입, 길이, 범위 검증은 Request DTO의 Bean Validation으로 처리한다.
- DB 조회, 중복, 상태 전이, 소유권 같은 도메인 규칙은 Service 또는 Domain에서 `ApiException`으로 처리한다.
- Controller에서 `try/catch`로 에러 응답을 만들지 않는다.
- 클라이언트에 노출하면 안 되는 내부 예외 메시지는 그대로 내려주지 않는다.

## 6. Validation 응답

Request body 검증 실패는 `MethodArgumentNotValidException`으로 처리한다.

Query parameter, path variable, model attribute 검증 실패는 `ConstraintViolationException` 또는 binding 계열 예외로 처리한다.

검증 실패 응답은 `error.details.errors`에 field별 메시지를 담는다.

## 7. traceId

`RequestTraceFilter`가 요청마다 traceId를 확정한다.

동작:

- 요청 헤더 `X-Request-Id`가 있으면 해당 값을 사용한다.
- 없으면 서버에서 UUID를 생성한다.
- 확정된 값은 request attribute에 저장한다.
- 응답 헤더 `X-Request-Id`로 다시 내려준다.
- 성공/실패 응답의 `traceId`에 같은 값을 넣는다.

운영 이슈를 추적할 때는 클라이언트 응답의 `traceId`와 서버 로그의 `traceId`를 기준으로 확인한다.

## 8. 보안 예외

`ApiSecurityExceptionHandler`는 Spring Security 예외를 공통 JSON 응답으로 변환한다.

- 인증되지 않은 요청: `UNAUTHORIZED`
- 인증은 되었지만 권한이 없는 요청: `FORBIDDEN`

API 요청에서 HTML redirect 응답을 내려주지 않는다.

## 9. 신규 코드 작성 규칙

- 에러 응답 포맷을 Controller마다 만들지 않는다.
- 도메인 실패는 `ApiException` 또는 더 구체적인 도메인 예외로 올리고 공통 핸들러가 변환하게 한다.
- validation 실패는 Bean Validation에 맡긴다.
- 인증/권한 실패는 Spring Security 흐름을 따른다.
- 운영 응답에 SQL, token, 개인정보, 내부 exception message를 노출하지 않는다.
- traceId를 Controller나 Service에서 직접 만들지 않는다.

## 10. 후속 작업

아직 미구현이거나 확정이 필요한 항목:

- rate limit 정책
- 파일 업로드 용량 초과 예외 매핑
- DB 예외 세분화
- OpenAPI error schema 반영
- 운영 로그와 traceId 연동 고도화
