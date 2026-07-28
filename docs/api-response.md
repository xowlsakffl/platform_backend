# API 응답 / 페이지네이션 규칙

작성 기준: 2026-07-28

이 문서는 Medi 백엔드의 JSON API 응답 포맷과 페이지네이션 사용 규칙을 정리한다. 구현은 Spring Boot 방식으로 한다.

## 1. 적용 범위

일반 JSON API는 `com.medi.common.web.ApiResponse`를 통해 응답한다.

- 컨트롤러의 일반 성공 응답은 `ApiResponse.success()`를 사용한다.
- `/api/*` 보안 실패는 `ApiSecurityExceptionHandler`가 공통 에러 응답으로 변환한다.
- MVC/Validation/애플리케이션 예외는 `GlobalExceptionHandler`가 공통 에러 응답으로 변환한다.
- 파일 다운로드, 스트리밍 다운로드처럼 JSON이 아닌 응답은 예외다.

## 2. 성공 응답

성공 응답은 항상 아래 필드를 포함한다.

```json
{
  "success": true,
  "data": {},
  "meta": null,
  "traceId": "request-trace-id"
}
```

규칙:

- `success`: 항상 `true`
- `data`: 응답 본문. 값이 없으면 `null`
- `meta`: 페이지네이션/부가 정보. 값이 없으면 `null`
- `traceId`: `RequestTraceFilter`가 확정한 요청 추적 ID

컨트롤러 예시:

```java
@GetMapping("/app-config")
ApiResponse.Success<AppConfigResponse> getAppConfig(HttpServletRequest request) {
    return ApiResponse.success(
        new AppConfigResponse(brandName, serviceName),
        RequestTrace.traceId(request)
    );
}
```

목록 응답은 `data`에 row 배열, `meta`에 페이지 정보를 둔다.

```json
{
  "success": true,
  "data": [],
  "meta": {
    "current_page": 1,
    "per_page": 15,
    "total": 0,
    "last_page": 1
  },
  "traceId": "request-trace-id"
}
```

## 3. Trace ID

`RequestTraceFilter`는 모든 요청에 trace ID를 부여한다.

- 요청 header `X-Request-Id`가 있으면 그대로 사용한다.
- 없으면 UUID를 생성한다.
- 응답 header `X-Request-Id`에도 같은 값을 내려준다.
- request attribute와 MDC `traceId`에 저장한다.

컨트롤러나 예외 핸들러에서 trace ID가 필요하면 `RequestTrace.traceId(request)`를 사용한다.

## 4. 에러 응답

에러 응답은 `ApiResponse.error()`와 `ApiError`를 사용한다.

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
- `error.code`: `ErrorCode` enum 값
- `error.message`: 클라이언트에 노출 가능한 메시지
- `error.details`: 상세 정보가 있을 때만 포함
- `traceId`: 요청 추적 ID

현재 공통 에러 코드는 다음을 기준으로 시작한다.

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

## 5. Validation 실패

Validation 실패는 `error.details.errors`에 필드별 오류를 담는다.

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

`GlobalExceptionHandler`는 다음 예외를 우선 공통 포맷으로 변환한다.

- `ApiException`
- `MethodArgumentNotValidException`
- `ConstraintViolationException`
- `HttpMessageNotReadableException`
- `MissingServletRequestParameterException`
- `HttpRequestMethodNotSupportedException`
- `AccessDeniedException`
- 기타 `Exception`

도메인/애플리케이션 계층에서 클라이언트에 알려야 하는 실패는 `ApiException`을 사용한다.

```java
throw new ApiException(ErrorCode.CONFLICT, "이미 승인된 신청입니다.");
```

## 6. Controller / Use Case 응답 책임

컨트롤러는 HTTP 요청/응답 adapter다.

- 요청 바인딩
- Bean Validation
- 인증 주체 전달
- use case 호출
- `ApiResponse` 연결

use case는 HTTP 응답 포맷을 알지 않는다. 비즈니스 결과 DTO 또는 command 결과만 반환한다.

조회 API 예시:

```java
HospitalDetail result = getHospitalForStaffUseCase.get(id);

return ApiResponse.success(Map.of("hospital", result), RequestTrace.traceId(request));
```

Command API 예시:

```java
approveHospitalEntryUseCase.approve(command);

return ApiResponse.success(
    Map.of("message", "승인되었습니다."),
    RequestTrace.traceId(request)
);
```

단순 완료 메시지는 컨트롤러에서 만든다. use case가 화면 문구를 만들지 않는다.

## 7. Length-aware pagination

Spring Data `Page<T>`를 쓰는 목록 응답은 `PaginatedResponse.from()`을 사용한다.

```java
PaginatedResponse<HospitalRow> result = PaginatedResponse.from(
    page,
    HospitalRow::from
);

return ApiResponse.success(result.items(), result.meta(), RequestTrace.traceId(request));
```

`PaginatedResponse.from()`은 아래 구조를 만든다.

```java
new PaginatedResponse<>(
    items,
    PageMeta.of(currentPage, perPage, total, lastPage)
);
```

외부 응답의 `current_page`는 1-based다. Spring Data 내부 `Page`는 0-based이므로 응답 변환 시 `page.getNumber() + 1`로 변환한다.

추가 meta가 필요하면 세 번째 인자로 전달한다.

```java
PaginatedResponse<HospitalRow> result = PaginatedResponse.from(
    page,
    HospitalRow::from,
    Map.of("summary", summary)
);
```

직접 `current_page`, `per_page`, `total`, `last_page`를 반복 조립하지 않는다.

## 8. Cursor pagination 예외

채팅 메시지 목록처럼 cursor 방식인 목록은 length-aware pagination에 태우지 않는다.

```json
{
  "per_page": 30,
  "has_more": true,
  "after_id": null,
  "before_id": 123,
  "order": "desc"
}
```

cursor 방식 목록을 추가할 때는 `current_page`, `total`, `last_page` 개념이 없는 별도 meta 구조를 명시적으로 유지한다.
